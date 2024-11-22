@import com.reposilite.ui.views.IndexView
@import com.reposilite.ui.views.IndexView.Project

@param com.reposilite.ui.views.IndexView view

<!DOCTYPE html>
<html lang="en">
    @template.component.Head(title = view.getTitle())

    <body class="min-h-full text-black dark:text-white min-w-full bg-zinc-100 dark:bg-zinc-900">
        @template.component.Header(title = view.getTitle())

        <main class="container mx-auto pt-1">
            <div class="flex min-h-[128px]">
                <div class="w-32">
                    <a href="">
                        <img alt="Logo" src="${view.getLogo()}" class="rounded-md border-2 border-zinc-200 dark:border-zinc-700" />
                    </a>
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
            @if (view.getBrowsedRepository() == null)
                <div class="flex justify-between flex-wrap w-full pt-14 gap-2">
                    @for(Project project : view.getHighlightedProjects())
                        <div class="
                            flex flex-1 flex-col justify-between
                            min-w-1/3 max-w-1/3 px-4 pt-3 pb-4
                            border rounded-md border-white shadow-sm dark:border-zinc-700
                            bg-white dark:bg-zinc-800
                            hover:transform hover:scale-[1.01] transition-transform cursor-pointer
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
                                <div class="flex flex-col">
                                    @for(IndexView.Repository repository : group.getRepositories())
                                        <a href="/browse/${repository.getName()}" class="text-black dark:text-white">
                                            <div class="flex flex-1 items-center gap-2 hover:bg-zinc-200 dark:hover:bg-zinc-900 rounded-md px-3.5 cursor-pointer">
                                                <div class="w-2 h-2 rounded-md
                                                    @if(repository.getType().equals("Generic"))
                                                         bg-zinc-300
                                                    @elseif(repository.getType().equals("Maven"))
                                                         bg-orange-300
                                                    @elseif(repository.getType().equals("Docker"))
                                                         bg-blue-300
                                                    @elseif(repository.getType().equals("NPM"))
                                                         bg-yellow-300
                                                    @endif
                                                ">
                                                </div>
                                                <p class="px-2 py-1.5 text-[0.93rem]">
                                                    ${repository.getName()}
                                                </p>
                                            </div>
                                        </a>
                                    @endfor
                                </div>
                            </div>
                        @endfor
                    </div>
                </div>
            @else
                <div class="flex justify-between pt-12">
                    <p class="">
                        Index of /${view.getBrowsedRepository().getName()}
                        <a href="/">
                            <span class="font-normal text-xl text-gray-500 select-none inter-500"> ⤴ </span>
                        </a>
                    </p>
<%--                    <div>--%>
<%--                        @template.lib.icons.ViewGrid()--%>
<%--                    </div>--%>
                </div>
                <div class="flex gap-2 mt-3">
                    <div class="flex flex-1 h-fit flex-col bg-white dark:bg-zinc-800 rounded-md px-3 py-3">
                        @for(var entry : view.getBrowsedRepository().getFiles())
                            <div class="py-1 flex items-center gap-3 hover:bg-zinc-200 dark:hover:bg-zinc-700 dark:hover:bg-zinc-900 rounded-md px-3 cursor-pointer">
                                @if (entry instanceof IndexView.BrowsedRepository.Directory directory)
                                    <div class="bg-yellow-300 w-2 h-2 mt-0.5 rounded-full"></div>
                                    <p>${directory.getName()}</p>
                                @elseif (entry instanceof IndexView.BrowsedRepository.File file)
                                    <div class="bg-zinc-300 w-2 h-2 mt-0.5 rounded-full"></div>
                                    <p>${file.getName()}</p>
                                @endif
                            </div>
                        @endfor
                    </div>
                    <div class="flex w-2/5 flex-col">
                        <div class="bg-white dark:bg-zinc-800 px-5 py-4 rounded-md h-fit">
                            <h2 class="text-sm font-semibold py-0.5">Repository details</h2>
                            <nav class="flex justify-between pt-4">
                                <button class="flex-1 py-2.5 text-sm rounded-t-md text-center bg-zinc-100 dark:bg-zinc-900 font-semibold">Maven</button>
                                <button class="flex-1 py-2.5 text-sm rounded-t-md text-center">Gradle Kotlin</button>
                                <button class="flex-1 py-2.5 text-sm rounded-t-md text-center">Gradle Groovy</button>
                                <button class="flex-1 py-2.5 text-sm rounded-t-md text-center">SBT</button>
                            </nav>
                            <div>
                                <xmp class="font-mono bg-zinc-100 dark:bg-zinc-900 px-4 py-4 my-0 mb-1 rounded-b-md text-xs"><repository>
    <id>reposilite-repository-${view.getBrowsedRepository().getName().toLowerCase()}</id>
    <name>Reposilite Repository</name>
    <url>https://maven.reposilite.com/${view.getBrowsedRepository().getName()}</url>
</repository></xmp>
                            </div>
                        </div>
                        <div class="flex justify-center bg-white dark:bg-zinc-800 h-2">
                            <div class="flex-1 bg-zinc-100 dark:bg-zinc-900 rounded-r-full"></div>
                            <div class="w-[97.5%] h-2"></div>
                            <div class="flex-1 bg-zinc-100 dark:bg-zinc-900 rounded-l-full"></div>
                        </div>
                        <div class="bg-white dark:bg-zinc-800 px-5 py-4 rounded-md h-fit">
                            <h2 class="text-sm font-semibold py-0.5">Artifact details</h2>
                            <div>
                                <xmp class="font-mono bg-zinc-100 dark:bg-zinc-900 px-4 py-4 rounded-md text-xs"><dependency>
    <groupId>com.reposilite</groupId>
    <artifactId>reposilite</artifactId>
    <version>4.0.0</version>
</dependency></xmp>
                            </div>
                        </div>
                    </div>
                </div>
            @endif

<%--            <button hx-post="/clicked" hx-swap="innerHTML">--%>
<%--                Click me--%>
<%--            </button>--%>

<%--            @template.component.Foo()--%>
        </main>

        @template.component.Footer()
    </body>
</html>