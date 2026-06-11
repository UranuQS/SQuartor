class ImportedFont {
  const ImportedFont({required this.name, required this.path});

  final String name;
  final String path;

  Map<String, Object?> toJson() => {'name': name, 'path': path};

  factory ImportedFont.fromJson(Map<String, Object?> json) {
    return ImportedFont(
      name: json['name'] as String? ?? '自定义字体',
      path: json['path'] as String? ?? '',
    );
  }
}
