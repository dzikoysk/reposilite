import type { Package } from "./item";

export interface ItemMeta {
    item: string;
    snippet: string;
    desc: string;
}

export const loadData = async (pkg: Package) => {
    const raw = await fetch("/data/" + pkg.dokkaLink).then((v) => v.text());
    const parser = new DOMParser();
    const html = parser.parseFromString(raw, "text/html");
    const main = html.getElementById("content") as HTMLDivElement;
    const descEl = main.querySelector("div.cover > div.platform-hinted > div.content > p") as HTMLParagraphElement;
    const desc = descEl.innerText;
    const table = main.querySelector("div.table");
    const rows = table?.querySelectorAll("div.table-row")!;
    const meta: Record<string, ItemMeta> = {};

    for (const row of rows) {
        const a = row.querySelector("span.inline-flex > div > a") as HTMLAnchorElement;
        const name = a.innerText;

        if (!(name in pkg.items)) {
            continue;
        }

        const title = row.querySelector("div > .title")!;
        const content = title.querySelector(".content")!;
        const codeEl = content.querySelector(".symbol") as HTMLDivElement;
        const descEl = content.querySelector(".brief") as HTMLDivElement;
        const code = codeEl.innerText;
        const desc = descEl.innerText;

        meta[name] = {
            item: name,
            snippet: code,
            desc,
        };
    }

    return meta;
};
