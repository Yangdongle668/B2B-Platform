import { ThemeRegistry, themeComponent } from '@b2bweb/website-kit'
import { industrialTheme } from '@b2bweb/themes'
import { definitions } from '@b2bweb/component-schemas'

export const themeRegistry = new ThemeRegistry(definitions, [industrialTheme], event => console.warn(JSON.stringify(event)))
export const HeroView = themeComponent(themeRegistry.resolveComponent('industrial', 'hero')!)
export const HeaderView = themeComponent(themeRegistry.region('industrial', 'header')!)
export const FooterView = themeComponent(themeRegistry.region('industrial', 'footer')!)
