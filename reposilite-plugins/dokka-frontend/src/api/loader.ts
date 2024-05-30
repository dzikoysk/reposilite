import type { DokkaItem, ItemType, Package } from "./item";

export const load = async () => {
    const raw = await fetch("/data/navigation.html").then((v) => v.text());
    const parser = new DOMParser();
    const html = parser.parseFromString(raw, "text/html");
    const rawParts = html.getElementsByTagName("div")[0].children;
    const parts = [];

    for (const part of rawParts) {
        if (part.className == "sideMenuPart") {
            parts.push(part);
        }
    }

    const pkgs: Record<string, Package> = {};

    for (const part of parts) {
        const a = part.querySelector(".overview > a") as HTMLAnchorElement;
        const pkg = a.innerText.trim();
        const dokkaLink = a.getAttribute("href")!; // use getAttrribute here so we get the relative URL
        const items: Record<string, DokkaItem> = {}; 

        for (const item of part.querySelectorAll(".sideMenuPart")) {
            const a = item.querySelector("a") as HTMLAnchorElement;
            const grid = a.querySelector(".nav-link-grid")!;
            const children = grid.querySelectorAll(".nav-link-child");
            const iconChild = children[0] as HTMLSpanElement;
            const child = children[children.length - 1] as HTMLSpanElement;
            const name = child.innerText.trim();
            const className = iconChild.className.replace("nav-link-child nav-icon", "").trim();
            const dokkaLink = a.getAttribute("href")!; // use getAttrribute here so we get the relative URL
            const deprecated = child.querySelectorAll("strike").length > 0;

            items[name] = {
                deprecated,
                dokkaLink,
                name,
                type: className as ItemType,
                pkg,
            };
        }

        pkgs[pkg] = {
            name: pkg,
            items,
            dokkaLink,
        };
    }

    return pkgs;
};
