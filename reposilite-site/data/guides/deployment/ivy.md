---
id: ivy
title: Ivy
---

### Ivy

For the best Ivy experience, configure Reposilite repositories as &lt;dual&gt; resolvers with both subresolvers pointing to the same repository.  This allows Ivy to resolve both dynamic revisions (2.+, [1.0,2.0], latest.[status], etc) and module configurations.

```xml
<resolvers>
    <dual name="reposilivy">
        <url name="repoivyfiles" m2compatible="true">
            <ivy pattern="http://hostname/releases/[organisation]/[module]/[revision]/ivy-[revision].xml"/>
        </url>
        <ibiblio name="repoartifacts" m2compatible="true" root="http://hostname/releases/"/>
    </dual>
</resolvers>
```

If supporting Ivy configurations is not needed, Reposilite repositories may be configured as a simple &lt;ibiblio&gt; resolver.  Dynamic revisions will work as expected, however Ivy will be unable to resolve module configurations.

```xml
<resolvers>
  <ibiblio name="reposilivy" m2compatible="true" root="http://hostname/releases/"/>
</resolvers>
```

To publish or access private repositories, add a &lt;credentials&gt; element to your ivysettings.xml file with your Reposilite user name and generated token.

```xml
  <credentials host="hostname" realm="Reposilite" username="username" passwd="auth-token"/>
```

If everything is setup correctly, resolve, publish, and other Ivy Ant tasks will work as expected.

As noted in the Ivy documentation, resolvers in m2compatible mode, are not able list available organizations. It means some features like <b>repreport</b> are not available.

Requires Reposilite 3.5.16 or later.
