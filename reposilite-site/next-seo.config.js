const URL = process.env.NODE_ENV === "production"
  ? process.env.SITE_URL
  : "http://localhost:3000/"

export default {
  title: 'Reposilite',
  description: 'Lightweight and easy-to-use repository manager for Maven based artifacts in JVM ecosystem. This is simple, extensible and scalable self-hosted solution to replace managers like Nexus, Archiva or Artifactory, with reduced resources consumption.',
  openGraph: {
    type: 'website',
    locale: 'en_US',
    url: URL,
    site_name: 'Reposilite',
    images: [
      { url: 'https://user-images.githubusercontent.com/4235722/133891983-966e5c6d-97b1-48cc-b754-6e88117ee4f7.png' }
    ]
  },
  twitter: {
    handle: '@dzikoysk',
    site: '@dzikoysk',
    cardType: 'summary_large_image'
  },
};