---
id: dashboard
title: Dashboard
---

Reposilite exposes a frontend that is mounted to `/#/` as [SPA _(single-page application)_](https://en.wikipedia.org/wiki/Single-page_application) in [Vue 3](https://vuejs.org/). You can disable the frontend in [shared configuration](/guide/settings#shared-configuration) if you want to disable an easy way to query your repository or to provide a custom frontend implementation using [static files](/guide/static-files).

#### Browser
Browser displays the content of the currently selected directory in respect to the provided authentication credentials. For users not logged in it lists only public files, for authenticated users it also displays files that the user has access to.

![Dashboard Browser Preview](/images/guides/dashboard-browser-preview.png)

You can control how Reposilite displays directory content by opening the adjustments view via the icon in the top-right corner:

![Browser Adjustments](/images/guides/dashboard-browser-adjustments.png)

#### Console

CLI _(command-line interface)_ displays the current Reposilite output and allows to perform available [commands](/guide/standalone#interactive-cli). Only access tokens with management permission can access this view.

![CLI Preview](/images/guides/dashboard-console.png)

You can also filter those messages using filters, but keep in mind that it searches only through the currently cached log entries, not the whole history.

#### Shared configuration
Shared configuration has been already mentioned in:

* [Guide / Setup :: Shared configuration](/guide/settings#shared-configuration)

Currently, configuration is provided in [CDN _(Configuration Data Notation)_](https://github.com/dzikoysk/cdn) format:

![Dashboard / Configuration](/images/guides/web-interface-configuration.png)

You can modify it and reload changes. Most of the properties support hot-reloading,
so you should see changes immediately _(visual updates after reloading the page)_.
