/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  theme: {
    extend: {
      colors: {
        // Palette militaire moderne (Call of Duty style)
        military: {
          dark: '#0a0e0f',
          darker: '#1a1f21',
          base: '#1e2326',
          lighter: '#2a3033',
          accent: '#3d4549',
        },
        tactical: {
          green: '#3d5a3c',
          olive: '#4a5f3a',
          camo: '#5a6b4a',
        },
        warning: {
          red: '#c1272d',
          orange: '#d97706',
          yellow: '#fbbf24',
        },
        hud: {
          blue: '#00b4d8',
          cyan: '#0dcaf0',
          teal: '#06b6d4',
        }
      },
      fontFamily: {
        military: ['Rajdhani', 'Orbitron', 'sans-serif'],
        tactical: ['Share Tech Mono', 'monospace'],
      },
      backgroundImage: {
        'camo-pattern': "url('data:image/svg+xml,%3Csvg width=\"60\" height=\"60\" viewBox=\"0 0 60 60\" xmlns=\"http://www.w3.org/2000/svg\"%3E%3Cg fill=\"none\" fill-rule=\"evenodd\"%3E%3Cg fill=\"%23000000\" fill-opacity=\"0.05\"%3E%3Cpath d=\"M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z\"/%3E%3C/g%3E%3C/g%3E%3C/svg%3E')",
        'grid-pattern': "url('data:image/svg+xml,%3Csvg width=\"100\" height=\"100\" xmlns=\"http://www.w3.org/2000/svg\"%3E%3Cdefs%3E%3Cpattern id=\"grid\" width=\"10\" height=\"10\" patternUnits=\"userSpaceOnUse\"%3E%3Cpath d=\"M 10 0 L 0 0 0 10\" fill=\"none\" stroke=\"%23ffffff\" stroke-width=\"0.5\" opacity=\"0.1\"/%3E%3C/pattern%3E%3C/defs%3E%3Crect width=\"100\" height=\"100\" fill=\"url(%23grid)\" /%3E%3C/svg%3E')",
      },
      boxShadow: {
        'tactical': '0 0 20px rgba(0, 180, 216, 0.3)',
        'tactical-lg': '0 0 40px rgba(0, 180, 216, 0.5)',
        'warning': '0 0 20px rgba(193, 39, 45, 0.4)',
        'inner-glow': 'inset 0 0 20px rgba(0, 180, 216, 0.1)',
      },
      animation: {
        'pulse-slow': 'pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'scan': 'scan 2s linear infinite',
        'glitch': 'glitch 1s linear infinite',
      },
      keyframes: {
        scan: {
          '0%': { transform: 'translateY(-100%)' },
          '100%': { transform: 'translateY(100%)' },
        },
        glitch: {
          '0%, 100%': { transform: 'translate(0)' },
          '33%': { transform: 'translate(-2px, 2px)' },
          '66%': { transform: 'translate(2px, -2px)' },
        },
      },
    },
  },
  plugins: [],
}

