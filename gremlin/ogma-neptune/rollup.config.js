import nodeResolve from "@rollup/plugin-node-resolve";

export default {
  input: "./src/index.js",
  output: {
    file: "./dist/index-bundle.js",
    format: "iife",
    name: "ogmaNeptune",
  },
  plugins: [nodeResolve()],
};
