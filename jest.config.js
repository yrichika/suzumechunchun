
// https://jestjs.io/docs/en/configuration.html

// vue-jest is not compatible with babel7 for now.
// babel.config.js will be ignored.

module.exports = {
  moduleFileExtensions: ['js', 'json', 'vue', 'ts'],
  transform: {
    '^.+\\.(vue)$': 'vue-jest',
    '^.+\\.(ts)$': 'ts-jest',
    // '.+\\.(css|styl|less|sass|scss|png|jpg|ttf|woff|woff2)$': 'jest-transform-stub',
    '^.+\\.js$': '<rootDir>/node_modules/babel-jest'
  },
  moduleNameMapper: {
    '@app/(.+)': '<rootDir>/resources/ts/$1',
    '@css/(.+)': '<rootDir>/resources/css/$1',
    '@test/(.+)': '<rootDir>/resources/test/$1'
  },
  snapshotSerializers: ['<rootDir>/node_modules/jest-serializer-vue'],
  testMatch: ["<rootDir>/resources/test/**/*.(spec|test).[jt]s"],

  transformIgnorePatterns: [
    '<rootDir>/node_modules/'
  ],

};