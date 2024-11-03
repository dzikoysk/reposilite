@param String title

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <!--suppress HtmlUnknownTarget -->
    <link rel="icon" href="/favicon.png" type="image/x-icon">
    <title>${title}</title>

    @template.lib.Theme()
    @template.lib.HTMX()
    @template.lib.Tailwind()
    @template.lib.DevMode()
    @template.lib.fonts.Inter()
    @template.lib.fonts.OpenSans()

    <style>
        #theme-switcher span {
            display: none;
        }
        .dark #theme-switcher .sun-icon {
            display: inline !important;
        }
        .light #theme-switcher .moon-icon {
            display: inline !important;
        }
    </style>
</head>