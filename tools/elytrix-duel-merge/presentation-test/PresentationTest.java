import java.util.*;
import dev.epicduels.gui.PresentationText;
import dev.epicduels.i18n.Messages;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
public class PresentationTest {
 public static void main(String[] args){
  Map<String,String> values=Map.of("arena","arena1","player","arena1");
  String legacy=PlainTextComponentSerializer.plainText().serialize(PresentationText.render("{arena} / {player}",values));
  String mini=PlainTextComponentSerializer.plainText().serialize(PresentationText.render("<arena> / <player>",values));
  if(!legacy.equals("АРЕНА / arena1") || !mini.equals("АРЕНА / arena1"))throw new AssertionError(legacy+" | "+mini);
  String tagged=PlainTextComponentSerializer.plainText().serialize(MiniMessage.miniMessage().deserialize("<arena>",Messages.unparsed("arena","arena1")));
  if(!tagged.equals("АРЕНА"))throw new AssertionError(tagged);
  System.out.println("Actual presentation and message placeholders passed");
 }
}
