<script>
const eventSource = new EventSource("/refreshDevMode");
eventSource.addEventListener("refresh", (data) => {
    console.info(data)
    location.reload()
})
</script>