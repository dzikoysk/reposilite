@param String title

<header class="border-b-1 border-gray-50 flex items-center w-full py-10">
    <div class="mx-auto container flex justify-between w-full">
        <nav class="font-medium text-xl">
            <a href="/">${title}</a>
        </nav>
        <nav>
            <button id="theme-switcher" class="flex bg-white dark:bg-zinc-800 rounded-full px-2 py-2">
                <span class="sun-icon color-white">
                    @template.lib.icons.SunIcon()
                </span>
                <span class="moon-icon color-black">
                    @template.lib.icons.MoonIcon()
                </span>
            </button>
        </nav>
    </div>
    <script>
        document.getElementById("theme-switcher").addEventListener("click", () => {
            document.documentElement.classList.toggle("dark")
            document.documentElement.classList.toggle("light")
            localStorage.setItem("color-theme", document.documentElement.classList.contains("dark") ? "dark" : "light")
        })
    </script>
</header>