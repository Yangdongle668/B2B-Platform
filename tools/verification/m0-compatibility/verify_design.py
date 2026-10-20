#!/usr/bin/env python3
"""Check document links, module schemas, dependency directions and DR-01–04 invariants."""
import hashlib
import json
from pathlib import Path
import re
from jsonschema import Draft202012Validator

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[2]


def main():
    errors = []
    framework = (REPO / 'docs/framework/platform-framework.md').read_text()
    schema = json.loads(framework.split('### A.2 JSON Schema')[1].split('```json\n')[1].split('\n```')[0])
    validator = Draft202012Validator(schema)
    modules = {}
    permissions, components, endpoints, acceptance = set(), set(), 0, 0
    for path in sorted((REPO / 'docs/modules').glob('*.md')):
        if path.stem == 'README':
            continue
        text = path.read_text()
        descriptor = text.split('### 2.1 ')[1].split('```json\n')[1].split('\n```')[0]
        data = json.loads(descriptor)
        modules[path.stem] = data
        for error in validator.iter_errors(data):
            errors.append(f'{path.name}: {error.message}')
        if re.findall(r'^## (\d+)\.', text, re.M) != list(map(str, range(1, 15))):
            errors.append(f'{path.name}: missing/reordered required chapter')
        for row in data['permissions']:
            if row['code'] in permissions:
                errors.append('Duplicate permission: ' + row['code'])
            permissions.add(row['code'])
        for component in data['components']:
            if component in components:
                errors.append('Duplicate component: ' + component)
            components.add(component)
        endpoints += len(re.findall(r'^\| (GET|POST|PUT|DELETE|PATCH) \|', text.split('### 6.1 ')[1].split('### 6.2 ')[0], re.M))
        acceptance += len(re.findall(r'^\| A\d+ \|', text, re.M))
    rules = framework.split('#### 4.1.3 调用规则')[1].split('#### 4.1.4')[0]
    allowed = set(re.findall(r'^\| ([a-z]+) → ([a-z]+)(?:（[^|]+）)? \|', rules, re.M))
    for name, data in modules.items():
        for dep in [*data['requires'], *data['optional']]:
            if dep not in modules:
                errors.append(f'{name}: unknown dependency {dep}')
            elif data['layer'] == 'L2' and (name, dep) not in allowed:
                errors.append(f'{name}: forbidden L2 direction to {dep}')
        def walk(node, stack):
            if node in stack:
                errors.append('Dependency cycle: ' + ' -> '.join(stack + [node]))
                return
            if node in modules:
                for child in [*modules[node]['requires'], *modules[node]['optional']]:
                    walk(child, stack + [node])
        walk(name, [])
    documents = sorted((REPO / 'docs').rglob('*.md')) + [REPO / 'CLAUDE.md', HERE / 'README.md']
    hashes = {}
    for path in documents:
        if not path.exists():
            errors.append('Missing document: ' + str(path.relative_to(REPO)))
            continue
        text = path.read_text()
        hashes[str(path.relative_to(REPO))] = hashlib.sha256(path.read_bytes()).hexdigest()
        if len(re.findall(r'^```', text, re.M)) % 2:
            errors.append(f'{path.name}: unbalanced code fences')
        for target in re.findall(r'\]\(([^)]+)\)', text):
            if '://' in target or target.startswith('#'):
                continue
            dest = target.split('#')[0]
            if dest and not (path.parent / dest).is_file() and not (path.parent / dest).is_dir():
                errors.append(f'{path.name}: missing linked file {target}')
    normative = [REPO / 'docs/architecture/system-design.md', REPO / 'docs/framework/platform-framework.md']
    normative += [REPO / 'docs/modules' / (name + '.md') for name in modules]
    normative += list((REPO / 'docs/adr').glob('*.md'))
    for path in normative:
        text = path.read_text()
        for invalid in ('cnt_localization.translation_review_required', 's-maxage=86400', 's-maxage=3600', 's-maxage=60', 'stale-while-revalidate=604800', 'stale-if-error=604800', '`event_publication`'):
            if invalid in text:
                errors.append(f'{path.name}: obsolete contract {invalid}')
    for required in ('LocalizationInfo lockDraftForWrite(long localizationId, int expectedVersion)',
                     'ContentInfo setTranslationReviewRequirement(long contentId, int expectedContentVersion, boolean required)',
                     'record GatedDownloadCommand', 'expectedTargetVersion', 'purge_job_id', 'download_grant'):
        if required not in framework:
            errors.append('Missing canonical contract: ' + required)
    result = dict(modules=len(modules), permissions=len(permissions), components=len(components),
                  admin_endpoints=endpoints, acceptance_cases=acceptance, errors=errors, documents_sha256=hashes)
    output = HERE / '.results'
    output.mkdir(exist_ok=True)
    (output / 'design.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps({k: v for k, v in result.items() if k != 'documents_sha256'}, ensure_ascii=False, indent=2))
    raise SystemExit(bool(errors))


if __name__ == '__main__':
    main()
