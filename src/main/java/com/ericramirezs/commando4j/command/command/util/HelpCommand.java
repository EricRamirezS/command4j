/*
 *
 *    Copyright 2022 Eric Bastian Ramírez Santis
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.ericramirezs.commando4j.command.command.util;

import com.ericramirezs.commando4j.command.CommandEngine;
import com.ericramirezs.commando4j.command.arguments.CommandArgument;
import com.ericramirezs.commando4j.command.arguments.IArgument;
import com.ericramirezs.commando4j.command.arguments.StringArgument;
import com.ericramirezs.commando4j.command.arguments.UnionArgument;
import com.ericramirezs.commando4j.command.command.Command;
import com.ericramirezs.commando4j.command.command.ICommand;
import com.ericramirezs.commando4j.command.exceptions.DuplicatedArgumentNameException;
import com.ericramirezs.commando4j.command.util.LocalizedFormat;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HelpCommand extends Command {
    private static SampleCommand sampleCommand;

    static {
        try {
            sampleCommand = new SampleCommand();
        } catch (DuplicatedArgumentNameException ignored) {
            //Command has no arguments
        }
    }

    private boolean simpleList = false;

    public HelpCommand() throws DuplicatedArgumentNameException {
        super("help",
                "util",
                "Command_Help_Description",
                new UnionArgument("commandName", "Command_Help_Argument_CommandNamePrompt")
                        .addArgumentType(new StringArgument("", "")
                                .addValidValues("all"))
                        .addArgumentType(new CommandArgument("", ""))
        );
        UnionArgument arg = (UnionArgument) getArguments().get(0);
        arg.setPromptParser(x -> localizePrompt(arg, x));

        addExamples("help",
                "help all",
                "help prefix");
    }

    private @NotNull String localizePrompt(UnionArgument arg, Event event) {
        if (event == null) return LocalizedFormat.format(arg.getPromptRaw());
        return LocalizedFormat.format(arg.getPromptRaw(), event);
    }

    @Override
    public String getDescription() {
        return LocalizedFormat.format(super.getDescription());
    }

    @Override
    public String getDescription(Event event) {
        return LocalizedFormat.format(super.getDescription(), event);
    }

    public HelpCommand useSimpleCommandList() {
        simpleList = true;
        return this;
    }

    @Override
    public void run(@NotNull MessageReceivedEvent event, @NotNull Map<String, IArgument> args) {
        UnionArgument argument = (UnionArgument) args.get("commandName");
        ICommand command = null;
        StringBuilder baseMessage = new StringBuilder();
        if (argument.getValue() instanceof CommandArgument c) command = c.getValue();
        boolean showAll = argument.getValue() instanceof StringArgument;

        if (command != null) {
            baseMessage = new StringBuilder(LocalizedFormat.format("Help_CommandSingle", event,
                    command.getName(event),
                    command.getDescription(event),
                    usageLocationLimit(command, event),
                    command.isNsfw() ? LocalizedFormat.format("Help_NSFW", event) : "",
                    command.anyUsage(event)));
            if (command.getAliases().size() > 0)
                baseMessage.append("\n").append(LocalizedFormat.format("Help_Aliases",
                        event,
                        String.join(", ", command.getAliases())));

            baseMessage.append("\n").append(LocalizedFormat.format("Help_Group", event, command.getGroup()));

            if (command.getDetails() != null)
                baseMessage.append("\n").append(LocalizedFormat.format("Help_Details", event, command.getDetails()));
            if (command.getExamples().size() > 0)
                baseMessage.append("\n").append(LocalizedFormat.format("Help_Examples",
                        event,
                        String.join("\n", command.getExamples())));
        } else {
            List<ICommand> commands = CommandEngine.getInstance().getCommands();
            if (!showAll) {
                commands = commands.stream().filter(c -> c.checkPermissions(event) == null).toList();
            }
            List<String> groups = commands.stream().map(ICommand::getGroup).distinct().sorted().toList();

            baseMessage.append(LocalizedFormat.format("Help_CommandList", event,
                    event.isFromGuild() ? event.getGuild().getName()
                            : LocalizedFormat.format("Help_AnyServer", event),
                    sampleCommand.anyUsage(event),
                    Objects.requireNonNull(CommandEngine.getInstance().getCommand("prefix"))
                            .usage("~", event)
            ));
            if (!event.isFromGuild())
                baseMessage.append("\n")
                        .append(LocalizedFormat.format("Help_DirectMessage", event, sampleCommand.getName(event)));

            baseMessage.append("\n\n")
                    .append(LocalizedFormat.format("Help_DetailedExample", event,
                            usage("<" + sampleCommand.getName(event) + ">", event)));

            if (!showAll)
                baseMessage.append("\n")
                        .append(LocalizedFormat.format("Help_UseAll", event, usage("all", event)));

            baseMessage.append("\n\n")
                    .append("__**")
                    .append(showAll ? LocalizedFormat.format("Help_AllCommands", event) :
                            LocalizedFormat.format("Help_Available", event,
                                    event.isFromGuild() ? event.getGuild().getName()
                                            : LocalizedFormat.format("Help_ThisDm", event)))
                    .append("**__")
                    .append("\n");
            for (String groupName : groups) {
                List<ICommand> groupCommands = commands.stream().filter(c -> Objects.equals(c.getGroup(), groupName)).toList();
                if (simpleList) {
                    baseMessage.append(String.format("\n`%s`:\n`%s`\n",
                            groupName,
                            String.join("`, `", groupCommands.stream().map(c -> getName(event)).toList())));
                } else {
                    baseMessage.append(String.format("\n`%s`:\n%s\n",
                            groupName,
                            String.join("\n", groupCommands.stream()
                                    .map(c -> String.format("**%s**: %s %s", c.getName(event)
                                                    , c.getDescription(event)
                                                    , c.isNsfw() ? '*' + LocalizedFormat.format("Help_NSFW", event) + '*' : "")
                                            .trim()
                                            .replace("\n", "\n\t"))
                                    .toList())));
                }
            }
        }
        String[] chunks = baseMessage.toString().split("(?<=\\G.{2000})");
        for (String message : chunks) {
            sendReply(event, baseMessage.toString());
        }
    }

    private @NotNull String usageLocationLimit(@NotNull ICommand c, Event event) {
        if (c.isThreadOnly()) {
            return "(" + LocalizedFormat.format("Help_ThreadOnly", event) + ")";
        }
        if (c.isGuildOnly()) {
            return "(" + LocalizedFormat.format("Help_GuildOnly", event) + ")";
        }
        if (c.isPrivateUseOnly()) {
            return "(" + LocalizedFormat.format("Help_UserOnly", event) + ")";
        }
        return "";
    }
}

final class SampleCommand extends Command {
    SampleCommand() throws DuplicatedArgumentNameException {
        super("command", "utils", "");
    }

    public @NotNull String getName(Event event) {
        return CommandEngine.getInstance().getString("NormalText_Command", CommandEngine.getInstance().getLanguage(event));
    }

    @Override
    public void run(@NotNull MessageReceivedEvent event, @NotNull Map<String, IArgument> args) {
        //do nothing
    }
}