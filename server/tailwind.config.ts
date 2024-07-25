import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./src/pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        content: '#282c34',
        background: '#21252b',
        body: '#abb2bf',
        hover: '#45484f',
      },
      fontFamily: {
        "icons": ["Segoe Fluent Icons"],
        "logs": ['var(--font-logs)'],
      },
    },
  },
  plugins: [
    require('tailwind-scrollbar')({ nocompatible: true }),
  ],
};
export default config;
