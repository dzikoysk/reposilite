@import com.reposilite.ui.views.IndexView
@import com.reposilite.ui.views.IndexView.Project

@param com.reposilite.ui.views.IndexView view

<!DOCTYPE html>
<html lang="en">
    @template.component.Head(title = "Reposilite")

    <body class="min-h-full text-black dark:text-white min-w-full bg-zinc-100 dark:bg-zinc-900">
        @template.component.Header(title = view.getTitle())

        <main class="container mx-auto">
            <div class="flex">
                <div class="w-32">
                    <img alt="Logo" src="${view.getLogo()}" class="rounded-md border-2 border-zinc-200 dark:border-zinc-700" />
                </div>
                <div class="flex flex-col pl-6 justify-center">
                    <h1>${view.getDescription()}</h1>
                    <p class="flex pt-3">
                        @template.lib.icons.GlobeIcon()
                        <a href="${view.getWebsite()}" class="text-zinc-500 px-2">
                            ${view.getWebsite()}
                        </a>
                    </p>
                </div>
            </div>
            <div class="flex justify-between flex-wrap w-full pt-12 gap-2">
                @for(Project project : view.getHighlightedProjects())
                    <div class="flex flex-1 flex-col justify-between border rounded-md border-white shadow-sm dark:border-zinc-700 min-w-1/3 max-w-1/3 px-4 pt-3 pb-4 bg-white dark:bg-zinc-800">
                        <div class="flex-col">
                            <h2 class="font-bold">${project.getName()}</h2>
                            <p class="text-wrap break-normal text-sm pt-1">${project.getDescription()}</p>
                        </div>
                        <div class="flex pt-3 gap-2">
                            @for(String packageName : project.getSupportedPackages())
                                <p class="text-sm">
                                    @if(packageName.equals("Generic"))
                                        <span class="px-3 py-0.5 bg-zinc-500 text-black rounded-full">Generic</span>
                                    @elseif(packageName.equals("Maven"))
                                        <span class="px-3 py-0.5 bg-orange-500 text-black rounded-full">Maven</span>
                                    @elseif(packageName.equals("Docker"))
                                        <span class="px-3 py-0.5 bg-blue-500 text-black rounded-full">Docker</span>
                                    @elseif(packageName.equals("NPM"))
                                        <span class="px-3 py-0.5 bg-yellow-500 text-black rounded-full">NPM</span>
                                    @endif
                                </p>
                            @endfor
                        </div>
                    </div>
                @endfor
            </div>
            <div class="py-12">
                <hr class="border-zinc-200 dark:border-zinc-700" />
            </div>
            <div class="flex">
                <div class="flex flex-col gap-2">
                    @for(IndexView.Repository repository : view.getRepositories())
                        <div class="flex flex-1 bg-white dark:bg-zinc-800 rounded-md items-center min-w-44">
                            <div class="
                                @if(repository.getType().equals("Generic"))
                                     bg-zinc-500
                                @elseif(repository.getType().equals("Maven"))
                                     bg-orange-500
                                @elseif(repository.getType().equals("Docker"))
                                     bg-blue-500
                                @elseif(repository.getType().equals("NPM"))
                                     bg-yellow-500
                                @endif
                                w-1 h-full">
                            </div>
                            <a href="/${repository.getName()}" class="h-full w-full">
                                <div class="flex justify-between h-full w-full">
                                    <p class="px-5 py-3 text-sm font-bold">${repository.getName().toUpperCase()}</p>
                                    <div class="w-2 h-full
                                        @if(view.getBrowsedRepository() != null && repository.getName().equals(view.getBrowsedRepository().getName()))
                                             bg-white dark:bg-zinc-800
                                        @else
                                             bg-zinc-100 dark:bg-zinc-900
                                        @endif
                                    "></div>
                                </div>
                            </a>
                        </div>
                    @endfor
                </div>
                <div class="flex flex-col w-full">
                    @if (view.getBrowsedRepository() == null)
<%--                        <input class="w-full bg-white dark:bg-zinc-800 rounded-sm py-2.5 px-3" placeholder="🔍 Search" />--%>
                    @else
                        <div class="bg-white dark:bg-zinc-800 w-fit px-4 py-2 rounded-md min-w-44">
                            @for(IndexView.BrowsedRepository.Entry entry : view.getBrowsedRepository().getFiles())
                                @if (entry instanceof IndexView.BrowsedRepository.Directory directory)
                                    <div class="py-1 flex items-center gap-3">
                                        <div class="bg-yellow-300 w-2 h-2 mt-0.5 rounded-full"></div>
                                        <p>${directory.getName()}</p>
                                    </div>
                                @elseif (entry instanceof IndexView.BrowsedRepository.File file)
                                    <div class="py-1 flex items-center gap-3">
                                        <div class="bg-white w-2 h-2 mt-0.5 rounded-full"></div>
                                        <p>${file.getName()}</p>
                                    </div>
                                @endif
                            @endfor
                        </div>
                    @endif
                </div>
            </div>

<%--            <button hx-post="/clicked" hx-swap="innerHTML">--%>
<%--                Click me--%>
<%--            </button>--%>

<%--            @template.component.Foo()--%>
        </main>

        @template.component.Footer()
    </body>
</html>