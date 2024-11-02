<div id="app"></div>

<script type="importmap">
    {
      "imports": {
        "preact": "https://esm.sh/preact@10.24.3",
        "preact/": "https://esm.sh/preact@10.24.3/",
        "htm/preact": "https://esm.sh/htm@3.1.1/preact?external=preact"
      }
    }
</script>

@raw
<script type="module">
import { render } from 'preact';
import { useState } from 'preact/hooks';
import { html } from 'htm/preact';

export function App() {
    const [count, setCount] = useState(0);
    return html`<button onClick=${() => setCount(count + 1)}>Increment ${count}</button>`;
}

render(html`<${App}><//>`, document.getElementById("app"))
</script>
@endraw