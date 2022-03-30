---
id: dashboard
title: Dashboard
---

Reposilite exposes frontend that is mounted to `/#/` as SPA application in Vue 3.
You can disable frontend in [shared configuration](/guide/setup#shared-configuration)
if you just want to disable an easy way to query your repository or to provide custom frontend implementation using [static files](/guide/static-files).

#### Browser
Browser displays content of currently selected directory, respectively to provided authentication credentials. For not logged user it lists only public files, for authenticated one it also displays files that user has access to.

![Dashboard Browser Preview](/images/guides/dashboard-browser-preview.png)

You can control the way Reposilite displays directory content by opening adjustments view through the icon in right top corner:

![Browser Adjustments](/images/guides/dashboard-browser-adjustments.png)

#### Console

CLI *(command-line interface)* displays current Reposilite output and allows to perform available [commands](/guide/manual#interactive-cli).
Only access tokens with management permission can access this view.

![CLI Preview](/images/guides/dashboard-console.png)

You can also filter those messages using filters, but keep in mind it searches only though the currently cached log entries, not the whole history.

#### Shared configuration
Shared configuration has been already mentioned in:

* [Guide / Setup :: Shared configuration](/guide/setup#shared-configuration)

Currently, configuration is provided in CDN format:

![Dashboard / Configuration](/images/guides/web-interface-configuration.png)

You can modify it and reload changes, most of the properties support hot-reloading,
so you should see changes immediately _(visual updates after reloading the page)_.
