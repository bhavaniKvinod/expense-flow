module.exports = {
  // jsdom gives tests a browser-like DOM for React Testing Library.
  testEnvironment: 'jsdom',
  // Loads @testing-library/jest-dom matchers (toBeInTheDocument, etc.).
  setupFilesAfterEnv: ['<rootDir>/jest.setup.cjs'],
  // Test files live next to source under src/, named *.test.js(x).
  testMatch: ['<rootDir>/src/**/*.test.{js,jsx}'],
  // Stub out CSS imports so component tests don't choke on them.
  moduleNameMapper: {
    '\\.(css|less|scss)$': '<rootDir>/test/styleMock.cjs',
  },
}
