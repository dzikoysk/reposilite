const available = !'{{REPOSILITE.BASE_PATH}}'.includes('REPOSILITE.BASE_PATH')

window.REPOSILITE_BASE_PATH = available ? '{{REPOSILITE.BASE_PATH}}' : '/'
window.REPOSILITE_ID = available ? '{{REPOSILITE.ID}}' : 'reposilite-repository'
window.REPOSILITE_TITLE = available ? '{{REPOSILITE.TITLE}}' : 'Reposilite Repository'
window.REPOSILITE_DESCRIPTION = available ? '{{REPOSILITE.DESCRIPTION}}' : 'Default description'