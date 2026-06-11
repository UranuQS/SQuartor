import 'dart:ui';

enum AppThemeId { wine, blue, green, brown, purple, black }

enum ReaderBackgroundId { theme, warm, black, paper, green }

enum AppBrightnessMode { system, light, dark }

class AppPalette {
  const AppPalette({
    required this.id,
    required this.label,
    required this.background,
    required this.surface,
    required this.card,
    required this.cardAlt,
    required this.line,
    required this.primary,
    required this.primarySoft,
    required this.text,
    required this.muted,
    required this.subtle,
    required this.blueMuted,
  });

  final AppThemeId id;
  final String label;
  final Color background;
  final Color surface;
  final Color card;
  final Color cardAlt;
  final Color line;
  final Color primary;
  final Color primarySoft;
  final Color text;
  final Color muted;
  final Color subtle;
  final Color blueMuted;

  bool get isLight => background.computeLuminance() > .5;
  Color get accentText => isLight ? primary : primarySoft;
}

class ThemeSeed {
  const ThemeSeed({required this.id, required this.label, required this.color});

  final AppThemeId id;
  final String label;
  final Color color;
}

const themeSeeds = <AppThemeId, ThemeSeed>{
  AppThemeId.wine: ThemeSeed(
    id: AppThemeId.wine,
    label: '酒红',
    color: Color(0xFFB00046),
  ),
  AppThemeId.blue: ThemeSeed(
    id: AppThemeId.blue,
    label: '深蓝',
    color: Color(0xFF2F6FA8),
  ),
  AppThemeId.green: ThemeSeed(
    id: AppThemeId.green,
    label: '墨绿',
    color: Color(0xFF2E7D5F),
  ),
  AppThemeId.brown: ThemeSeed(
    id: AppThemeId.brown,
    label: '暖棕',
    color: Color(0xFF9B522F),
  ),
  AppThemeId.purple: ThemeSeed(
    id: AppThemeId.purple,
    label: '紫黑',
    color: Color(0xFF7D4CC2),
  ),
  AppThemeId.black: ThemeSeed(
    id: AppThemeId.black,
    label: '纯黑',
    color: Color(0xFF5B6472),
  ),
};

class ReaderPalette {
  const ReaderPalette({
    required this.id,
    required this.label,
    required this.background,
    required this.text,
    required this.muted,
  });

  final ReaderBackgroundId id;
  final String label;
  final Color background;
  final Color text;
  final Color muted;
}

const appPalettes = <AppThemeId, AppPalette>{
  AppThemeId.wine: AppPalette(
    id: AppThemeId.wine,
    label: '酒红',
    background: Color(0xFF120B10),
    surface: Color(0xFF1E1118),
    card: Color(0xFF2B1821),
    cardAlt: Color(0xFF351B27),
    line: Color(0xFF553141),
    primary: Color(0xFFB00046),
    primarySoft: Color(0xFFFF9FBD),
    text: Color(0xFFF8EDF2),
    muted: Color(0xFFC9B8C0),
    subtle: Color(0xFF8E7C85),
    blueMuted: Color(0xFFA9D5FF),
  ),
  AppThemeId.blue: AppPalette(
    id: AppThemeId.blue,
    label: '深蓝',
    background: Color(0xFF0A1017),
    surface: Color(0xFF111B25),
    card: Color(0xFF172838),
    cardAlt: Color(0xFF1D3347),
    line: Color(0xFF32495F),
    primary: Color(0xFF2F6FA8),
    primarySoft: Color(0xFFA9D5FF),
    text: Color(0xFFEAF3FF),
    muted: Color(0xFFB8C7D6),
    subtle: Color(0xFF7890A4),
    blueMuted: Color(0xFFA9D5FF),
  ),
  AppThemeId.green: AppPalette(
    id: AppThemeId.green,
    label: '墨绿',
    background: Color(0xFF08120F),
    surface: Color(0xFF101D18),
    card: Color(0xFF172A22),
    cardAlt: Color(0xFF1F372D),
    line: Color(0xFF315748),
    primary: Color(0xFF2E7D5F),
    primarySoft: Color(0xFFA7E6CE),
    text: Color(0xFFEDF8F3),
    muted: Color(0xFFB9CFC5),
    subtle: Color(0xFF799387),
    blueMuted: Color(0xFFB3D8FF),
  ),
  AppThemeId.brown: AppPalette(
    id: AppThemeId.brown,
    label: '暖棕',
    background: Color(0xFF150D09),
    surface: Color(0xFF22150F),
    card: Color(0xFF321F16),
    cardAlt: Color(0xFF43291C),
    line: Color(0xFF5C4032),
    primary: Color(0xFF9B522F),
    primarySoft: Color(0xFFFFC49F),
    text: Color(0xFFFFF0E5),
    muted: Color(0xFFD5BFB0),
    subtle: Color(0xFF9B8270),
    blueMuted: Color(0xFFC2DDFF),
  ),
  AppThemeId.purple: AppPalette(
    id: AppThemeId.purple,
    label: '紫黑',
    background: Color(0xFF100D18),
    surface: Color(0xFF191426),
    card: Color(0xFF261C38),
    cardAlt: Color(0xFF31244A),
    line: Color(0xFF4D3D68),
    primary: Color(0xFF7D4CC2),
    primarySoft: Color(0xFFD1B2FF),
    text: Color(0xFFF4EEFF),
    muted: Color(0xFFC7BADB),
    subtle: Color(0xFF8C7BA5),
    blueMuted: Color(0xFFB9DCFF),
  ),
  AppThemeId.black: AppPalette(
    id: AppThemeId.black,
    label: '纯黑',
    background: Color(0xFF090A0D),
    surface: Color(0xFF111318),
    card: Color(0xFF1A1D23),
    cardAlt: Color(0xFF232832),
    line: Color(0xFF383D46),
    primary: Color(0xFF5B6472),
    primarySoft: Color(0xFFD5DDE9),
    text: Color(0xFFF2F5FA),
    muted: Color(0xFFC4CBD6),
    subtle: Color(0xFF858D99),
    blueMuted: Color(0xFFA9D5FF),
  ),
};

const readerPalettes = <ReaderBackgroundId, ReaderPalette>{
  ReaderBackgroundId.theme: ReaderPalette(
    id: ReaderBackgroundId.theme,
    label: '主题',
    background: Color(0xFF17100D),
    text: Color(0xFFEADFD8),
    muted: Color(0xFFB7A8A0),
  ),
  ReaderBackgroundId.warm: ReaderPalette(
    id: ReaderBackgroundId.warm,
    label: '暖棕',
    background: Color(0xFF17100D),
    text: Color(0xFFEADFD8),
    muted: Color(0xFFB7A8A0),
  ),
  ReaderBackgroundId.black: ReaderPalette(
    id: ReaderBackgroundId.black,
    label: '黑色',
    background: Color(0xFF050506),
    text: Color(0xFFDEDEDE),
    muted: Color(0xFF9A9A9A),
  ),
  ReaderBackgroundId.paper: ReaderPalette(
    id: ReaderBackgroundId.paper,
    label: '米白',
    background: Color(0xFFF2E5CF),
    text: Color(0xFF30241B),
    muted: Color(0xFF806B58),
  ),
  ReaderBackgroundId.green: ReaderPalette(
    id: ReaderBackgroundId.green,
    label: '护眼',
    background: Color(0xFFDFEAD9),
    text: Color(0xFF223026),
    muted: Color(0xFF667564),
  ),
};

AppPalette buildPaletteFromSeed({
  required Color seed,
  required Brightness brightness,
  required String label,
  required AppThemeId id,
}) {
  if (brightness == Brightness.light) {
    return AppPalette(
      id: id,
      label: label,
      background: _mix(const Color(0xFFFFFBFC), seed, .055),
      surface: _mix(const Color(0xFFFFF4F7), seed, .06),
      card: _mix(const Color(0xFFFFFFFF), seed, .04),
      cardAlt: _mix(const Color(0xFFF6EDF2), seed, .105),
      line: _mix(const Color(0xFFEBDDE5), seed, .18),
      primary: seed,
      primarySoft: _mix(seed, const Color(0xFFFFFFFF), .44),
      text: const Color(0xFF141820),
      muted: const Color(0xFF66707C),
      subtle: const Color(0xFF8F98A5),
      blueMuted: const Color(0xFF2A78B8),
    );
  }
  return AppPalette(
    id: id,
    label: label,
    background: _mix(const Color(0xFF08070A), seed, .08),
    surface: _mix(const Color(0xFF111016), seed, .12),
    card: _mix(const Color(0xFF1B1218), seed, .18),
    cardAlt: _mix(const Color(0xFF251723), seed, .22),
    line: _mix(const Color(0xFF45313B), seed, .36),
    primary: seed,
    primarySoft: _mix(seed, const Color(0xFFFFFFFF), .54),
    text: const Color(0xFFF8F1F5),
    muted: const Color(0xFFC9BBC3),
    subtle: const Color(0xFF948690),
    blueMuted: const Color(0xFFA9D5FF),
  );
}

String themeHex(Color color) {
  String two(double value) {
    final channel = (value * 255).round().clamp(0, 255);
    return channel.toRadixString(16).padLeft(2, '0').toUpperCase();
  }

  return '#${two(color.r)}${two(color.g)}${two(color.b)}';
}

Color _mix(Color a, Color b, double amount) {
  final inverse = 1 - amount;
  return Color.fromARGB(
    255,
    ((a.r * 255 * inverse) + (b.r * 255 * amount)).round().clamp(0, 255),
    ((a.g * 255 * inverse) + (b.g * 255 * amount)).round().clamp(0, 255),
    ((a.b * 255 * inverse) + (b.b * 255 * amount)).round().clamp(0, 255),
  );
}
