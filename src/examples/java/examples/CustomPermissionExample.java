package examples;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.EricRamirezS.jdacommando.command.Slash;
import org.EricRamirezS.jdacommando.command.arguments.IArgument;
import org.EricRamirezS.jdacommando.command.command.Command;
import org.EricRamirezS.jdacommando.command.enums.Emoji;
import org.EricRamirezS.jdacommando.command.exceptions.DuplicatedArgumentNameException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Map;

public class CustomPermissionExample extends Command implements Slash {

    public CustomPermissionExample() throws DuplicatedArgumentNameException {
        super("permTest", "examples", "This command only works if the user's name contains an \"R\"");
    }

    @Override
    protected String hasPermission(MessageReceivedEvent event) {
        if (event.getAuthor().getName().contains("R")) return null;
        return "You cannot execute this command, only users whose nickname contains an \"R\" may execute it";
    }

    @Override
    protected String hasPermission(SlashCommandInteractionEvent event) {
        if (event.getUser().getName().contains("R")) return null;
        return "You cannot execute this command, only users whose nickname contains an \"R\" may execute it";
    }

    /* You may use the generic version of hasPermission to handle both cases in one method.
    @Override
    protected String hasPermission(Event event) {
        if (event instanceof MessageReceivedEvent e) {
            if (e.getAuthor().getName().contains("R")) return null;
        } else if (event instanceof SlashCommandInteractionEvent e)
            if (e.getUser().getName().contains("R")) return null;
        return "You cannot execute this command, only users whose nickname contains an \"R\" may execute it";
    }*/

    @Override
    public void run(@NotNull MessageReceivedEvent event, @NotNull Map<String, IArgument> args) {
        sendReply(event,"Hey!, your name contains an \"R\", so you're cool " + Emoji.SUNGLASSES);
    }

    @Override
    public void run(@NotNull SlashCommandInteractionEvent event, @UnmodifiableView @NotNull Map<String, IArgument> args) {
        event.reply("Hey!, your name contains an \"R\", so you're cool " + Emoji.SUNGLASSES).setEphemeral(true).queue();
    }
}