package com.frivy.assistant.automation
import com.frivy.assistant.device.DeviceActionExecutor
class AutomationEngine(private val executor: DeviceActionExecutor) {
    fun executeSequence(commands: List<Pair<String,String>>): List<String> = commands.map { executor.execute(it.first,it.second) }
}