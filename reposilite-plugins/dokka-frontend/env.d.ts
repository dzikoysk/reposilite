/// <reference types="vite/client" />
/// <reference types="vite-svg-loader" />

export interface WindowExt extends Window {
    Prism: any;
}

declare var window: WindowExt;
