const CopyWebpackPlugin = require('copy-webpack-plugin');
const nodeExternals = require('webpack-node-externals');
const path = require('path');
module.exports = {
  entry: {
    index: './src/lambdas/index.ts',
  },
  externals: [nodeExternals()],
  mode: 'production',
  devtool: 'source-map',
  target: 'node',
  resolve: {
    extensions: ['.ts', '.js', '.json'],
    alias: {
      src: path.resolve(__dirname, 'src/'),
      tests: path.resolve(__dirname, 'tests/')
    }
  },
  module: {
    rules: [
      {
        test: /\.tsx?$/,
        loader: 'ts-loader'
      }
    ],
  },
  plugins: [
    new CopyWebpackPlugin({
      patterns: [{
        from: 'package.json',
        to: 'package.json'
      }]
    })
  ],
  output: {
    path: path.resolve(__dirname, 'build'),
    libraryTarget: 'commonjs2',
    filename: '[name].js',
    clean: true
  }
};
