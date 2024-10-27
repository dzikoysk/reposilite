<!DOCTYPE html>
<html lang="en">
    @template.component.Head(title = "Reposilite")

    <body class="min-h-full text-white min-w-full bg-black">
        @template.component.Header(title = "Reposilite")

        <button hx-post="/clicked" hx-swap="innerHTML">
            Click me
        </button>

        @template.component.Foo()

        @template.component.Footer()
    </body>
</html>