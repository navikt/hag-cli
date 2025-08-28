package no.nav.helse.cli.lpsapi

import java.sql.Connection
import java.sql.DriverManager

private const val USER_NAME = ""
private const val LPSAPI_URL = "jdbc:postgresql://localhost:55559/hag-lps"
private const val BRO_URL = "jdbc:postgresql://localhost:55557/helsearbeidsgiver-bro-sykepenger"

fun lpsapiConnection(): Connection {
    val url = LPSAPI_URL
    val user = USER_NAME
    val password = ""
    val conn = DriverManager.getConnection(url, user, password)
    conn.autoCommit = false
    conn.createStatement().execute("SET TRANSACTION READ ONLY")
    return conn
}

fun broConnection(): Connection {
    val url = BRO_URL
    val user = USER_NAME
    val password = ""
    val conn = DriverManager.getConnection(url, user, password)
    conn.autoCommit = false
    conn.createStatement().execute("SET TRANSACTION READ ONLY")
    return conn
}
