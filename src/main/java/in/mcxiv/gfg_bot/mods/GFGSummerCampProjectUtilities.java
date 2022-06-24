package in.mcxiv.gfg_bot.mods;

import in.mcxiv.gfg_bot.GFG_KIIT_Bot;
import in.mcxiv.gfg_bot.SpecialisedListenerAdapter;
import in.mcxiv.gfg_bot.utils.Utilities;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GFGSummerCampProjectUtilities extends SpecialisedListenerAdapter {

    // IMPORTANT, I have not used Category IDs and Channel IDs so that I can test these on another server too.

    private static final String CATEGORY_NAME = "summer project";

    private static final String CHANNEL_NAME = "projects";

    private final GFG_KIIT_Bot bot;

    public GFGSummerCampProjectUtilities(GFG_KIIT_Bot bot) {
        this.bot = bot;
    }

    private static boolean isMessageFromSummerCampProjectCategory(Channel channel) {
        if (!(channel instanceof ICategorizableChannel iCategorizableChannel)) return false;
        if (iCategorizableChannel.getParentCategory() == null) return false;
        if (!Utilities.allIn(iCategorizableChannel.getParentCategory().getName(), CATEGORY_NAME)) return false;
        return true;
    }

    private static boolean isMessageFromSummerCampProjectChannel(Channel channel) {
        if (!isMessageFromSummerCampProjectCategory(channel)) return false;
        if (!channel.getType().isGuild()) return false;
        if (!channel.getName().equals(CHANNEL_NAME)) return false;
        if (channel.getType() != ChannelType.TEXT) return false;
        return true;
    }

    @Override
    public boolean isSuperior() {
        return true;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!isMessageFromSummerCampProjectChannel(event.getChannel())) return;
        if (isTheMessageAChannelCommand(event)) return;

        var raw_content = bot.stripCommand(event.getMessage().getContentRaw())
                .strip().replaceAll("[\n\r]+", "\n");
        var lines = Utilities.simplifyContent(raw_content);
        Map<String, String> fields = linesDoesntContainAllTheRequiredInformation(lines, event);
        if (fields.isEmpty()) return;

        ChannelAction<TextChannel> channel = event.getGuild().createTextChannel(fields.get("Project's Name"));
        channel.setParent(((TextChannel) event.getChannel()).getParentCategory());
        channel.setTopic(fields.get("Project's Description"));
        channel.queue(textChannel -> sendProjectInfoEmbed(event.getMember(), textChannel, fields));
    }

    private boolean isTheMessageAChannelCommand(MessageReceivedEvent event) {
        String contentDisplay = event.getMessage().getContentDisplay();
        if (contentDisplay.contains("\n")) return false;
        if (!contentDisplay.contains("delete")) return false;
        List<GuildChannel> channels = event.getMessage().getMentions().getChannels();
        if (channels.isEmpty()) return false;
        channels = channels.stream().filter(GFGSummerCampProjectUtilities::isMessageFromSummerCampProjectCategory)
                .filter(guildChannel -> !guildChannel.getName().equals(CHANNEL_NAME)).toList();
        if (channels.isEmpty()) return true;
        if (!event.getMember().hasPermission(Permission.MANAGE_CHANNEL)) return true;
        channels.forEach(guildChannel -> guildChannel.delete().queue());
        return true;
    }

    private void sendProjectInfoEmbed(Member member, TextChannel textChannel, Map<String, String> fields) {
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle(fields.get("Project's Name"))
                .setColor(Color.BLUE);
        fields.remove("Project's Name");
        fields.forEach((k, v) -> builder.addField(k, v, false));
        builder.addField("Discord ID", member.getAsMention(), false);
        textChannel.sendMessageEmbeds(builder.build()).queue();
    }

    private Map<String, String> linesDoesntContainAllTheRequiredInformation(List<Utilities.SSPair> lines, MessageReceivedEvent event) {
        lines = new ArrayList<>(lines);
        ArrayList<String[]> missingFields = new ArrayList<>();
        LinkedHashMap<String, String> foundFields = new LinkedHashMap<>();

        List<Utilities.SSPair> dataItem = lines.stream().filter(spPair -> Utilities.isOneOf(spPair.s, "name")).toList();
        if (dataItem.size() == 1)
            foundFields.put("User Name", dataItem.get(0).m);
        else missingFields.add(new String[]{
                "Author Name",
                """
                    Name: Hello There! How should I address you?
                    How would you describe your programming experience?
                    
                    eg Name: Loistoa, Intermediate
                    """
        });
        lines.removeAll(dataItem);

        dataItem = lines.stream().filter(spPair -> Utilities.isOneOf(spPair.s, "domain", "project domain", "projects domain")).toList();
        if (dataItem.size() == 1)
            foundFields.put("Project's Domain", dataItem.get(0).m);
        else missingFields.add(new String[]{
                "Project Domain",
                """
                    Domain: Which domain your project comes under?
                    Possible values are @Web Dev, @ML/AI, @CP, @Content, @UI/UX, @Marketing and @Video Editing.
                    Feel free to mention something else.
                    
                    eg Domain: @Web Dev
                    eg Domain: App Development
                    """
        });
        lines.removeAll(dataItem);

        dataItem = lines.stream().filter(spPair -> Utilities.isOneOf(spPair.s, "project name", "projects name")).toList();
        if (dataItem.size() == 1)
            foundFields.put("Project's Name", dataItem.get(0).m);
        else missingFields.add(new String[]{
                "Project Name",
                """
                    Project Name: What is the name of your project?
                    
                    eg Project Name: Historical Monuments
                    """
        });
        lines.removeAll(dataItem);

        dataItem = lines.stream().filter(spPair -> Utilities.isOneOf(spPair.s, "description", "project description", "projects description", "desc", "project desc", "projects desc")).toList();
        if (dataItem.size() == 1)
            foundFields.put("Project's Description", dataItem.get(0).m);
        else missingFields.add(new String[]{
                "Project Description",
                """
                    Project Description: What does your project do?
                    
                    eg Project Description:
                        Tours and Travel Website.
                        Made using HTML CSS JAVASCRIPT.
                        I want help in modifying it and add frameworks.
                    """
        });
        lines.removeAll(dataItem);

        lines.forEach(ssPair -> foundFields.put(ssPair.s, ssPair.m));
        if (missingFields.isEmpty() || missingFields.size() == 4) return foundFields;
        foundFields.clear();

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle("Hello there! Welcome to GFG Summer Camp!")
                .setColor(Color.BLUE);
        missingFields.forEach(strings -> embedBuilder.addField(strings[0], "```\n%s\n```\n".formatted(strings[1]), false));

        event.getChannel().sendMessageEmbeds(embedBuilder.build()).queue();
        return foundFields;
    }

}
