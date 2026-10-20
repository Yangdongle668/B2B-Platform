#!/usr/bin/env python3
"""Build, then check SSR, a custom image provider and deferred hydration."""
import json
import os
from pathlib import Path
import shutil
import socket
import subprocess
import time
import urllib.request
import urllib.error
from playwright.sync_api import sync_playwright, expect

HERE = Path(__file__).resolve().parent
APP = HERE / 'frontend'
OUTPUT = HERE / '.results'


def main():
    OUTPUT.mkdir(exist_ok=True)
    (OUTPUT / 'frontend.json').unlink(missing_ok=True)
    env = os.environ.copy()
    env['NUXT_TELEMETRY_DISABLED'] = '1'
    if env.get('CODEX_PROXY_CERT'):
        env['NODE_EXTRA_CA_CERTS'] = env['CODEX_PROXY_CERT']
    for command in (['npm', 'ci', '--no-audit', '--no-fund'], ['npm', 'run', 'typecheck'], ['npm', 'run', 'build']):
        print(' '.join(command), flush=True)
        subprocess.run(command, cwd=APP, env=env, check=True)
    with socket.socket() as sock:
        sock.bind(('127.0.0.1', 0))
        port = sock.getsockname()[1]
    env.update(NITRO_HOST='127.0.0.1', NITRO_PORT=str(port))
    url = f'http://127.0.0.1:{port}'
    # Explicit loopback client: inherited internet proxies are irrelevant to this local server.
    local = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    texts = json.loads((APP / 'app/texts/zh-CN.json').read_text())['probe']
    with (OUTPUT / 'frontend-server.log').open('w') as log:
        server = subprocess.Popen(['node', '.output/server/index.mjs'], cwd=APP, env=env, stdout=log, stderr=subprocess.STDOUT)
        try:
            for _ in range(60):
                try:
                    html = local.open(url, timeout=2).read().decode()
                    break
                except urllib.error.HTTPError as error:
                    raise RuntimeError(f'Nuxt SSR failed: HTTP {error.code}; {error.read().decode()[-2000:]}') from error
                except OSError:
                    if server.poll() is not None:
                        raise RuntimeError('Nuxt server exited; inspect .results/frontend-server.log')
                    time.sleep(0.25)
            else:
                raise TimeoutError('Nuxt startup timed out')
            assert texts['title'] in html and f"{texts['increment']}: 0" in html
            assert 'probeWidth=320' in html and 'data-hydrated="false"' in html
            assert local.open(url + '/probe.png?probeWidth=320').status == 200
            checks = []
            with sync_playwright() as p:
                browser = p.chromium.launch(executable_path=os.environ.get('CHROMIUM_PATH') or shutil.which('chromium'))
                try:
                    for width in (1440, 768, 390):
                        page = browser.new_page(viewport={'width': width, 'height': 900})
                        errors = []
                        page.on('pageerror', lambda e: errors.append(str(e)))
                        page.on('console', lambda m: errors.append(m.text) if m.type == 'error' or 'hydration' in m.text.lower() and m.type == 'warning' else None)
                        page.goto(url, wait_until='networkidle')
                        expect(page.locator('h1')).to_have_text(texts['title'])
                        counter = page.get_by_test_id('counter')
                        expect(counter).to_have_attribute('data-hydrated', 'false')
                        counter.scroll_into_view_if_needed()
                        expect(counter).to_have_attribute('data-hydrated', 'true')
                        counter.click()
                        counter.click()
                        expect(counter).to_have_text(f"{texts['increment']}: 2")
                        assert page.locator('img').evaluate('(img) => img.complete && img.naturalWidth > 0')
                        assert not errors, errors
                        checks.append(dict(width=width, deferred_until_visible=True, click_count=2, browser_errors=errors))
                        page.close()
                    version = browser.version
                finally:
                    browser.close()
            result = dict(typecheck=True, production_build=True, ssr=True, custom_image_provider=True,
                          chromium=version, viewports=checks, node=subprocess.check_output(['node', '--version'], text=True).strip())
            (OUTPUT / 'frontend.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
            print(json.dumps(result, ensure_ascii=False, indent=2), flush=True)
        finally:
            server.terminate()
            try:
                server.wait(timeout=10)
            except subprocess.TimeoutExpired:
                server.kill()
                server.wait()


if __name__ == '__main__':
    main()
