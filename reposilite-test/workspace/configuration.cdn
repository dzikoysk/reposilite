# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ #
#       Reposilite :: Local       #
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ #

# Local configuration contains init params for current Reposilite instance.
# For more options, shared between instances, login to the dashboard with management token and visit 'Configuration' tab.

# Hostname
# The hostname can be used to limit which connections are accepted.
# Use 0.0.0.0 to accept connections from anywhere.
# 127.0.0.1 will only allow connections from localhost.
hostname: 0.0.0.0
# Port to bind
port: 8080
# Database configuration. Supported storage providers:
# - mysql localhost:3306 database user password
# - sqlite reposilite.db
# - sqlite --temporary
# Experimental providers (not covered with tests):
# - postgresql localhost:5432 database user password
# - h2 reposilite
database: sqlite reposilite.db

# Support encrypted connections
sslEnabled: true
# SSL port to bind
sslPort: 443
# Key file to use.
# You can specify absolute path to the given file or use ${WORKING_DIRECTORY} variable.
# If you want to use .pem certificate you need to specify its path next to the key path.
# Example .pem paths setup:
# keyPath: ${WORKING_DIRECTORY}/cert.pem ${WORKING_DIRECTORY}/key.pem
# Example .jks path setup:
# keyPath: ${WORKING_DIRECTORY}/keystore.jks
keyPath: ${WORKING_DIRECTORY}/cert.pem ${WORKING_DIRECTORY}/key.pem
# Key password to use
keyPassword: reposilite
# Redirect http traffic to https
enforceSsl: false

# Max amount of threads used by core thread pool (min: 5)
# The web thread pool handles first few steps of incoming http connections, as soon as possible all tasks are redirected to IO thread pool.
webThreadPool: 16
# IO thread pool handles all tasks that may benefit from non-blocking IO (min: 2)
# Because most of tasks are redirected to IO thread pool, it might be a good idea to keep it at least equal to web thread pool.
ioThreadPool: 8
# Database thread pool manages open connections to database (min: 1)
# Embedded databases such as SQLite or H2 don't support truly concurrent connections, so the value will be always 1 for them if selected.
databaseThreadPool: 1
# Select compression strategy used by this instance.
# Using 'none' reduces usage of CPU & memory, but ends up with higher transfer usage.
# GZIP is better option if you're not limiting resources that much to increase overall request times.
# Available strategies: none, gzip
compressionStrategy: none
# Default idle timeout used by Jetty
idleTimeout: 30000

# Adds cache bypass headers to each request from /api/* scope served by this instance.
# Helps to avoid various random issues caused by proxy provides (e.g. Cloudflare) and browsers.
bypassExternalCache: true
# Amount of messages stored in cached logger.
cachedLogSize: 50
# Enable default frontend with dashboard
defaultFrontend: true
# Set custom base path for Reposilite instance.
# It's not recommended to mount Reposilite under custom base path
# and you should always prioritize subdomain over this option.
basePath: /
# Debug mode
debugEnabled: false