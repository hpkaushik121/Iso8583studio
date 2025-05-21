package `in`.aicortex.iso8583studio.data

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import `in`.aicortex.iso8583studio.domain.service.GatewayServiceImpl
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class ExceptionHandler : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread?, throwable: Throwable?) {
        // Log the exception
        println("Uncaught exception in thread ${thread?.name}: ${throwable?.message}")
        throwable?.printStackTrace()

        // Perform custom error handling, e.g., show an error message or restart the app
        println("The application will terminate.")
    }
}


class IsoCoroutine(val gatewayServiceImpl: GatewayServiceImpl?,override val coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.IO) :
    CoroutineScope {
    var onError: ((Throwable) -> Unit)? = null
    fun launchSafely(
         onError: ((Throwable) -> Unit)? = null,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        this@IsoCoroutine.onError = onError
        return launch(context = coroutineContext) {
            try {
                block()
            } catch (e: Throwable) {
                e.printStackTrace()
                onException(e)
            }
        }
    }

    fun onException(e: Throwable){

        onError?.let {
            it(e)
        } ?: run {
            gatewayServiceImpl?.showError {
                Text("Error: ${e.message}")
            }
        }
    }

    fun cancel() {
        coroutineContext.cancelChildren()
    }
}


@Composable
fun rememberIsoCoroutineScope(gatewayServiceImpl: GatewayServiceImpl): IsoCoroutine {

    val scope = rememberCoroutineScope()

    return remember {
        IsoCoroutine(
            gatewayServiceImpl,
            scope.coroutineContext + SupervisorJob()
        )
    }
}