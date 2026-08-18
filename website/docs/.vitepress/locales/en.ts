import { createRequire } from 'module'
import { defineConfig } from 'vitepress'

const require = createRequire(import.meta.url)
const pkg = require('vitepress/package.json')

export default defineConfig({
  lang: 'en-US',
  description: 'A kernel-based root solution for Android GKI devices.',

  themeConfig: {
    nav: nav(),

    lastUpdatedText: 'Last updated',

    sidebar: {
      '/guide/': sidebarGuide()
    },

    socialLinks: [
      { icon: 'github', link: 'https://github.com/kelexine/mysu' }
    ],

    footer: {
        message: 'Released under the GPL-3.0 License.',
        copyright: 'Copyright © 2026 kelexine & MySU contributors.'
    },

    editLink: {
        pattern: 'https://github.com/kelexine/mysu/edit/main/website/docs/:path',
        text: 'Edit this page on GitHub'
    }
  }
})

function nav() {
  return [
    { text: 'Guide', link: '/guide/what-is-mysu' },
  ]
}

function sidebarGuide() {
  return [
    {
        text: 'Guide',
        items: [
          { text: 'What is MySU?', link: '/guide/what-is-mysu' },
          { text: 'Difference with Magisk', link: '/guide/difference-with-magisk' },
          { text: 'Installation', link: '/guide/installation' },
          { text: 'How to build', link: '/guide/how-to-build' },
          { text: 'Intergrate for non-GKI devices', link: '/guide/how-to-integrate-for-non-gki'},
          { text: 'Unofficially supported devices', link: '/guide/unofficially-support-devices.md' },
          { text: 'Module guide', link: '/guide/module.md' },
          { text: 'Metamodule', link: '/guide/metamodule.md' },
          { text: 'Module WebUI', link: '/guide/module-webui.md' },
          { text: 'Module Configuration', link: '/guide/module-config.md' },
          { text: 'App Profile', link: '/guide/app-profile.md' },
          { text: 'Rescue from bootloop', link: '/guide/rescue-from-bootloop.md' },
          { text: 'x86_64 support', link: '/guide/x86_64-support' },
          { text: 'FAQ', link: '/guide/faq' },
          { text: 'Hidden features', link: '/guide/hidden-features' },
        ]
    }
  ]
}
