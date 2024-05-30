export type ItemType =
    | "class"
    | "interface"
    | "abstract-class"
    | "enum-class"
    | "enum-class-kt"
    | "exception-class"
    | "annotation-class"
    | "function"
    | "val"
    | "var"
    | "class-kt"
    | "object"
    | "annotation-class-kt"
    | "interface-kt"
    | "abstract-class-kt"
    | "typealias-kt";

export interface DokkaItem {
    type: ItemType;
    name: string;
    pkg: string;
    deprecated: boolean;
    dokkaLink: string;
}

export interface Package {
    name: string;
    items: Record<string, DokkaItem>;
    dokkaLink: string;
}
