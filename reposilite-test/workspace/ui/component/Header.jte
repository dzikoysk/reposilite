@param String title

<header class="border-b-1 border-gray-50 flex items-center w-full py-10">
    <div class="mx-auto container flex justify-between w-full">
        <nav class="font-medium text-xl flex justify-between items-center w-full">
            <a href="/" class="w-1/5">
                ${title}
            </a>
            <div class="flex bg-white dark:bg-zinc-800 items-center rounded-md px-3 w-2/5">
                <div class="h-4 w-4">
                    @template.lib.icons.SearchIcon()
                </div>
                <input
                    class="w-full text-sm bg-white dark:bg-zinc-800 py-1.5 px-3 focus-visible:outline-none focus-visible:ring-0 placeholder:focus-visible:text-white"
                    placeholder="Search"
                />
            </div>
            <nav class="flex gap-2 w-1/5 justify-end">
                <button class="bg-white dark:bg-zinc-800 px-3 py-0.5 rounded-md text-sm">
                    Sign in
                </button>
                <button id="theme-switcher" class="flex bg-white dark:bg-zinc-800 rounded-md px-2 py-1.5">
                    <span class="sun-icon color-white">
                        @template.lib.icons.SunIcon()
                    </span>
                        <span class="moon-icon color-black">
                        @template.lib.icons.MoonIcon()
                    </span>
                </button>
            </nav>
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