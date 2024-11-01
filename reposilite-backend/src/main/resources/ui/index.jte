@param com.reposilite.ui.views.IndexView view

<!DOCTYPE html>
<html lang="en">
    @template.component.Head(title = "Reposilite")

    <body class="min-h-full text-black dark:text-white min-w-full bg-white dark:bg-black">
        @template.component.Header(title = view.getTitle())

        <button hx-post="/clicked" hx-swap="innerHTML">
            Click me
        </button>

        @template.component.Foo()

        @template.component.Footer()
    </body>
</html>