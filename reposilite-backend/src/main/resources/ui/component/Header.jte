@param String title

<header class="border-b-1 border-gray-50 h-16 flex items-center w-full">
    <div class="mx-auto container-xl flex justify-between w-full">
        <nav>
            ${title}
        </nav>
        <nav>
            <button id="theme-switcher" class="flex bg-gray-900 rounded px-4 py-1">
                Switch
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