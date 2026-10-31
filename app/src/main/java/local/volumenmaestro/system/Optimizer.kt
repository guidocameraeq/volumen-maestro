package local.volumenmaestro.system

import local.volumenmaestro.engine.Shell

/** Estado de la TV y la limpieza de "Optimizar ahora". */
object Optimizer {
    data class Stats(val memAvailableKb: Long, val memTotalKb: Long, val dataFreeKb: Long, val uptimeSec: Long)
    data class Result(val before: Stats, val after: Stats) {
        val freedStorageMb get() = ((after.dataFreeKb - before.dataFreeKb) / 1024).coerceAtLeast(0)
        val freedMemoryMb get() = ((after.memAvailableKb - before.memAvailableKb) / 1024).coerceAtLeast(0)
    }

    fun stats(sh: Shell.Channel): Stats {
        val out = sh.run("grep -E '^(MemTotal|MemAvailable):' /proc/meminfo; df -k /data | tail -1; cat /proc/uptime")
        val lines = out.lines()
        fun kb(key: String) = lines.firstOrNull { it.startsWith(key) }
            ?.let { Regex("(\\d+)").find(it)?.value?.toLong() } ?: 0L
        val df = lines.firstOrNull { it.startsWith("/") }?.trim()?.split(Regex("\\s+"))
        val dataFree = df?.getOrNull(3)?.toLongOrNull() ?: 0L
        val uptime = lines.lastOrNull { it.matches(Regex("^[0-9.]+ [0-9.]+$")) }
            ?.substringBefore('.')?.toLongOrNull() ?: 0L
        return Stats(kb("MemAvailable:"), kb("MemTotal:"), dataFree, uptime)
    }

    /** Libera caché de todas las apps y cierra las que quedaron en segundo plano. */
    fun run(sh: Shell.Channel): Result {
        val before = stats(sh)
        sh.run("pm trim-caches 999G")
        sh.run("am kill-all")
        Thread.sleep(2_000)
        return Result(before, stats(sh))
    }
}
