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
      text: 'Overview',
      items: [
        { text: 'What is MySU?', link: '/guide/what-is-mysu' },
        { text: 'Difference with Magisk', link: '/guide/difference-with-magisk' },
        { text: 'App Profile & Sandboxing', link: '/guide/app-profile' },
      ]
    },
    {
      text: 'Installation & Kernel Integration',
      items: [
        { text: 'Installation', link: '/guide/installation' },
        { text: 'How to Build', link: '/guide/how-to-build' },
        { text: 'Integrate for non-GKI Devices', link: '/guide/how-to-integrate-for-non-gki' },
        { text: 'x86_64 Support', link: '/guide/x86_64-support' },
        { text: 'Unofficially Supported Devices', link: '/guide/unofficially-support-devices' },
      ]
    },
    {
      text: 'Module Development',
      items: [
        { text: 'Module Guide', link: '/guide/module' },
        { text: 'Metamodule Architecture', link: '/guide/metamodule' },
        { text: 'Module WebUI', link: '/guide/module-webui' },
        { text: 'Module Configuration', link: '/guide/module-config' },
      ]
    },
    {
      text: 'Troubleshooting & Reference',
      items: [
        { text: 'Rescue from Bootloop', link: '/guide/rescue-from-bootloop' },
        { text: 'Hidden Features & CLI', link: '/guide/hidden-features' },
        { text: 'FAQ', link: '/guide/faq' },
      ]
    }
  ]
}
