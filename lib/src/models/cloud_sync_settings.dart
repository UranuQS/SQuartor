class CloudSyncSettings {
  const CloudSyncSettings({
    this.enabled = false,
    this.endpoint = '',
    this.username = '',
    this.password = '',
    this.lastUploadAt,
    this.lastDownloadAt,
  });

  final bool enabled;
  final String endpoint;
  final String username;
  final String password;
  final DateTime? lastUploadAt;
  final DateTime? lastDownloadAt;

  bool get configured {
    return endpoint.trim().isNotEmpty &&
        username.trim().isNotEmpty &&
        password.isNotEmpty;
  }

  CloudSyncSettings copyWith({
    bool? enabled,
    String? endpoint,
    String? username,
    String? password,
    DateTime? lastUploadAt,
    DateTime? lastDownloadAt,
    bool clearLastUploadAt = false,
    bool clearLastDownloadAt = false,
  }) {
    return CloudSyncSettings(
      enabled: enabled ?? this.enabled,
      endpoint: endpoint ?? this.endpoint,
      username: username ?? this.username,
      password: password ?? this.password,
      lastUploadAt: clearLastUploadAt
          ? null
          : lastUploadAt ?? this.lastUploadAt,
      lastDownloadAt: clearLastDownloadAt
          ? null
          : lastDownloadAt ?? this.lastDownloadAt,
    );
  }

  Map<String, Object?> toJson() => {
    'enabled': enabled,
    'endpoint': endpoint,
    'username': username,
    'password': password,
    'lastUploadAt': lastUploadAt?.toIso8601String(),
    'lastDownloadAt': lastDownloadAt?.toIso8601String(),
  };

  factory CloudSyncSettings.fromJson(Map<String, Object?> json) {
    return CloudSyncSettings(
      enabled: json['enabled'] as bool? ?? false,
      endpoint: json['endpoint'] as String? ?? '',
      username: json['username'] as String? ?? '',
      password: json['password'] as String? ?? '',
      lastUploadAt: DateTime.tryParse(json['lastUploadAt'] as String? ?? ''),
      lastDownloadAt: DateTime.tryParse(
        json['lastDownloadAt'] as String? ?? '',
      ),
    );
  }
}
