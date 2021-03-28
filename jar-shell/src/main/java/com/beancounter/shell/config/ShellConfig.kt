package com.beancounter.shell.config

import com.beancounter.client.config.ClientConfig
import com.beancounter.common.utils.UtilConfig
import com.beancounter.shell.cli.DataCommands
import com.beancounter.shell.cli.PortfolioCommands
import com.beancounter.shell.cli.ShellPrompt
import com.beancounter.shell.cli.UtilCommands
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(
    EnvConfig::class,
    UtilConfig::class,
    UtilCommands::class,
    ShellPrompt::class,
    PortfolioCommands::class,
    DataCommands::class,
    ClientConfig::class
)
open class ShellConfig
