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
            <div class="flex justify-between flex-wrap w-full pt-14 gap-2">
                @for(Project project : view.getHighlightedProjects())
                    <div class="
                        flex flex-1 flex-col justify-between
                        min-w-1/3 max-w-1/3 px-4 pt-3 pb-4
                        border rounded-md border-white shadow-sm dark:border-zinc-700
                        bg-white dark:bg-zinc-800
                    ">
                        <div class="flex-col">
                            <h2 class="font-bold">${project.getName()}</h2>
                            <p class="text-wrap break-normal text-sm pt-1">${project.getDescription()}</p>
                        </div>
                        <div class="flex pt-3 gap-2">
                            @for(String packageName : project.getSupportedPackages())
                                <p class="text-xs">
                                    @if(packageName.equals("Generic"))
                                        <span class="px-3 py-0.5 bg-zinc-300 text-black rounded-full">Generic</span>
                                    @elseif(packageName.equals("Maven"))
                                        <span class="px-3 py-0.5 bg-orange-300 text-black rounded-full">Maven</span>
                                    @elseif(packageName.equals("Docker"))
                                        <span class="px-3 py-0.5 bg-blue-300 text-black rounded-full">Docker</span>
                                    @elseif(packageName.equals("NPM"))
                                        <span class="px-3 py-0.5 bg-yellow-200 text-black rounded-full">NPM</span>
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
                <div class="flex flex-wrap items-center w-full gap-2">
                    @for(var group : view.getGroupedRepositories())
                        <div class="
                            flex flex-col flex-1 px-4 py-3 h-full
                            bg-white dark:bg-zinc-800 border border-white shadow-sm dark:border-zinc-700 rounded-md
                        ">
                            <h2 class="font-semibold pb-1.5">${group.getType()}</h2>
                            <div class="flex flex-col px-0.5">
                                @for(IndexView.Repository repository : group.getRepositories())
                                    <div class="flex flex-1 items-center gap-2">
                                        <div class="w-2 h-2 rounded-md
                                        @if(repository.getType().equals("Generic"))
                                             bg-zinc-300
                                        @elseif(repository.getType().equals("Maven"))
                                             bg-orange-300
                                        @elseif(repository.getType().equals("Docker"))
                                             bg-blue-300
                                        @elseif(repository.getType().equals("NPM"))
                                             bg-yellow-200
                                        @endif
                                    ">
                                        </div>
                                        <p class="px-2 py-1.5 text-sm">${repository.getName()}</p>
                                    </div>
                                @endfor
                            </div>
                        </div>
                    @endfor
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