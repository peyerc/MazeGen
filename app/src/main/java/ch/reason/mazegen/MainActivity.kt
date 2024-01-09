package ch.reason.mazegen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import ch.reason.mazegen.ui.theme.MazeGenTheme
import dev.ricknout.composesensors.accelerometer.rememberAccelerometerSensorValueAsState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MazeGenTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val sensorValue by rememberAccelerometerSensorValueAsState(
//                        samplingPeriodUs = SENSOR_DELAY_GAME,
                    )
                    val directions by remember {
                        derivedStateOf {
                            val (x, y, _) = sensorValue.value
                            val threshold = 1
                            val dirX = when {
                                x > threshold -> Direction.West
                                x < -threshold -> Direction.East
                                else -> null
                            }
                            val dirY = when {
                                y > threshold -> Direction.South
                                y < -threshold -> Direction.North
                                else -> null
                            }
                            listOfNotNull(dirX, dirY)
                        }
                    }

                    Maze(
                        width = 8,
                        height = 16,
                        directions = directions,
                        goalReached = {
                            println("Goal reached!!")
                        }
                    )
                }
            }
        }
    }
}
