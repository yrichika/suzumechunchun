module.exports = {
  purge: {
    // Set `enabled` value false, when you are editing twirl template files(`.scala.html` files).
    // Not sure why but tailwind purge sometimes ignores tailwind classes in `.scala.html` files.
    // For this project, if tailwind is working wired in any way, first thing to do is set `enabled` false.
    enabled: true,
    content: [
      './app/views/**/*.html',
      './resources/ts/**/*.html',
      './resources/ts/**/*.js',
      './resources/ts/**/*.ts',
      './resources/ts/**/*.vue',
    ],
    options: {
      // don't purge bg and border colors
      safelist: [/^bg-.*/, /^border-.*/]
    }
  },
  darkMode: 'media',
  theme: {
    extend: {},
  },
  variants: {
    extend: {
      opacity: ['disabled']
    },
  },
  plugins: [],
}
