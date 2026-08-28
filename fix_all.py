import re

def fix_file(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Remove all top-level Stitch colors
    content = re.sub(r'(private )?val Stitch[a-zA-Z0-9]+.*?Color\(.*?\)\n', '', content)
    
    # Add them back as private
    colors = """
private val StitchBg = Color(0xFF0F1115)
private val StitchCard = Color(0xFF1A1C23)
private val StitchRed = Color(0xFFE5534B)
private val StitchGreen = Color(0xFF20E070)
private val StitchPurple = Color(0xFF6A6CFF)
private val StitchTextMuted = Color(0xFFA0A0A5)
private val StitchDarkGray = Color(0xFF2A2A35)
private val StitchBottomNav = Color(0xFF13151A)
"""
    
    # Find the last import
    last_import = content.rfind("import ")
    end_of_line = content.find("\n", last_import)
    
    # Clean up bad Stitch references like StitchRed2, StitchRed3, etc
    content = re.sub(r'Stitch([a-zA-Z]+)[0-9]+', r'Stitch\1', content)
    
    # Fix alpha calls (e.g. StitchRed.copy(alpha = 0.5f) -> StitchRed.copy(alpha = 0.5f))
    # It seems there was an issue where Color.copy wasn't resolved, but it should be if it's explicitly androidx.compose.ui.graphics.Color
    # No, the error "No parameter with name 'alpha' found" happens when compiler thinks it's a completely different class (like a List or something)
    # Wait, the error was "Argument type mismatch: actual type is 'List<Any>', but 'List<Color>' was expected." at line 163 of DevicePairingScreen.kt.
    # Ah! In Brush.sweepGradient(colors = listOf(Color.Transparent, StitchPurple.copy(alpha = 0.4f)))
    # Because StitchPurple was Unresolved, the compiler inferred the list type as List<Any>, which conflicted with Brush.sweepGradient.
    # Once StitchPurple is resolved, this goes away!
    
    content = content[:end_of_line+1] + colors + content[end_of_line+1:]
    
    # Fix HomeScreen syntax error
    content = content.replace('text = "Hold central button for 3s to\\ntrigger emergency",', 'text = "Hold central button for 3s to\\ntrigger emergency",')
    content = content.replace('text = "Hold central button for 3s to\ntrigger emergency",', 'text = "Hold central button for 3s to\\ntrigger emergency",')
    
    with open(filepath, 'w') as f:
        f.write(content)

fix_file("app/src/main/java/com/example/ui/screens/HomeScreen.kt")
fix_file("app/src/main/java/com/example/ui/screens/EmergencyScreen.kt")
fix_file("app/src/main/java/com/example/ui/screens/DevicePairingScreen.kt")
