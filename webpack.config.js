const path = require('path');
const VueLoaderPlugin = require('vue-loader/lib/plugin');
const BrowserSyncPlugin = require('browser-sync-webpack-plugin');
const WebpackNotifier = require('webpack-notifier');
const mode = process.env.NODE_ENV;
const sourceMap = (mode == 'production') ? false : 'inline-source-map';

module.exports = {
    mode: mode, // 'production' will compress javascript file
    devtool: sourceMap,
    entry : {
        app: './resources/ts/main.ts',
    },
    module: {
        rules: [
            // typescript
            {
                test: /\.tsx?$/,
                loader: 'ts-loader',
                options: {
                    appendTsSuffixTo: [/\.vue$/],
                },
                exclude: /node_modules/,
            },
            // vue
            {
                test: /\.vue$/,
                loader: 'vue-loader'
            },
            // css
            {
                test: /\.(sa|sc|c)ss$/,
                use: [
                    "style-loader",
                    "css-loader",
                    // "postcss-loader"
                    {
                        loader: 'postcss-loader',
                        options: {
                            postcssOptions: {
                                // Things for postcss.config.js. Writing it here instead of postcss.conf.js
                                plugins: [
                                    'tailwindcss',
                                    'autoprefixer'
                                ]
                            }
                        }
                    }
                ],
            },
            // fonts
            {
                test: /\.(woff|woff2|eot|ttf|otf)$/i,
                type: 'asset/resource',
            },
        ]
    },
    resolve: {
        alias: {
            // necessary for vue: https://jp.vuejs.org/v2/guide/installation.html#ランタイム-コンパイラとランタイム限定の違い
            'vue$': 'vue/dist/vue.esm.js',
            '@app': path.resolve(__dirname, 'resources/ts'),
            '@css': path.resolve(__dirname, 'resources/css'),
            '@test': path.resolve(__dirname, 'resources/test')
        },
        extensions: ['.tsx', '.ts', '.js', '.vue'],
        fallback: {'crypto': false}
    },
    output: {
        filename: 'javascripts/[name].js',
        path: path.resolve(__dirname, 'public'),
        // this is for fonts export
        assetModuleFilename: 'fonts/[hash][ext][query]'
    },
    plugins: [
        new WebpackNotifier({alwaysNotify: true}),
        new VueLoaderPlugin(),
         new BrowserSyncPlugin({
             files: ['./app/**/*'], // also watches scala file changes
             // host: 'localhost', TODO: Setting proxy does not work well with Play's http server yet. Need some adjustments.
             // port: 3001,
             // proxy: 'http://localhost:9000',
             // browser: 'google chrome' // 'Chrome' for windows, 'google chrome' for Mac
         })
    ]
};