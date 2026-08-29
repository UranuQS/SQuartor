import 'dart:convert';
import 'dart:io';

import '../models.dart';

class CloudSyncException implements Exception {
  const CloudSyncException(this.message);

  final String message;

  @override
  String toString() => message;
}

class CloudSyncService {
  const CloudSyncService();

  static const syncFolderName = 'SQuartor';
  static const syncFileName = 'squartor-sync-v1.json';

  Future<void> upload({
    required CloudSyncSettings settings,
    required Map<String, Object?> payload,
  }) async {
    final bytes = utf8.encode(jsonEncode(payload));
    final uri = _syncUri(settings);
    _validateSettings(uri, settings);
    final client = _client();
    try {
      await _ensureRemoteFolder(client, settings);
      final request = await client.openUrl('PUT', uri);
      _authorize(request, settings);
      request.headers.contentType = ContentType.json;
      request.headers.contentLength = bytes.length;
      request.add(bytes);
      final response = await _close(request);
      await _ensureSuccess(response, '上传失败');
    } finally {
      client.close(force: true);
    }
  }

  Future<Map<String, Object?>> download(CloudSyncSettings settings) async {
    final uri = _syncUri(settings);
    _validateSettings(uri, settings);
    final client = _client();
    try {
      final request = await client.openUrl('GET', uri);
      _authorize(request, settings);
      final response = await _close(request);
      if (response.statusCode == HttpStatus.notFound) {
        await response.drain<void>();
        throw const CloudSyncException('远端还没有同步数据，请先上传本机数据');
      }
      await _ensureSuccess(response, '下载失败');
      final text = await utf8.decoder.bind(response).join();
      final decoded = jsonDecode(text);
      if (decoded is Map) {
        return decoded.cast<String, Object?>();
      }
    } finally {
      client.close(force: true);
    }
    throw const CloudSyncException('远端同步文件格式不正确');
  }

  void _validateSettings(Uri uri, CloudSyncSettings settings) {
    if (!settings.configured) {
      throw const CloudSyncException('请先填写 WebDAV 地址、账号和密码');
    }
    if (uri.scheme != 'http' && uri.scheme != 'https') {
      throw const CloudSyncException('WebDAV 地址需要以 http 或 https 开头');
    }
  }

  HttpClient _client() {
    return HttpClient()..connectionTimeout = const Duration(seconds: 12);
  }

  void _authorize(HttpClientRequest request, CloudSyncSettings settings) {
    request.headers.set(
      HttpHeaders.authorizationHeader,
      'Basic ${base64Encode(utf8.encode('${settings.username}:${settings.password}'))}',
    );
    request.headers.set(HttpHeaders.acceptHeader, 'application/json');
  }

  Future<HttpClientResponse> _close(HttpClientRequest request) {
    return request.close().timeout(const Duration(seconds: 25));
  }

  Future<void> _ensureRemoteFolder(
    HttpClient client,
    CloudSyncSettings settings,
  ) async {
    final folderUri = _syncFolderUri(settings);
    final request = await client.openUrl('MKCOL', folderUri);
    _authorize(request, settings);
    request.headers.contentLength = 0;
    final response = await _close(request);
    if (response.statusCode == HttpStatus.created ||
        response.statusCode == HttpStatus.ok ||
        response.statusCode == HttpStatus.noContent ||
        response.statusCode == HttpStatus.methodNotAllowed) {
      await response.drain<void>();
      return;
    }
    await _throwHttpFailure(response, '创建同步文件夹失败');
  }

  Future<void> _ensureSuccess(
    HttpClientResponse response,
    String action,
  ) async {
    if (response.statusCode >= 200 && response.statusCode < 300) {
      return;
    }
    await _throwHttpFailure(response, action);
  }

  Future<Never> _throwHttpFailure(
    HttpClientResponse response,
    String action,
  ) async {
    final detail = await utf8.decoder.bind(response).join();
    final code = response.statusCode;
    final friendly = switch (code) {
      HttpStatus.unauthorized || HttpStatus.forbidden => '账号或应用密码不正确，或没有写入权限',
      HttpStatus.notFound => '远端路径不存在，请确认 WebDAV 地址是否正确',
      HttpStatus.conflict => '父文件夹不存在，请检查 WebDAV 地址',
      HttpStatus.methodNotAllowed => '当前 WebDAV 地址不允许写入',
      _ => _compactErrorDetail(detail),
    };
    final suffix = friendly.isEmpty ? '' : '：$friendly';
    throw CloudSyncException('$action（HTTP $code）$suffix');
  }

  String _compactErrorDetail(String detail) {
    final text = detail
        .replaceAll(RegExp(r'<[^>]*>'), ' ')
        .replaceAll(RegExp(r'\s+'), ' ')
        .trim();
    if (text.isEmpty) {
      return '';
    }
    return text.length <= 90 ? text : '${text.substring(0, 90)}...';
  }

  Uri _syncUri(CloudSyncSettings settings) {
    return _syncFolderUri(settings).resolve(syncFileName);
  }

  Uri _syncFolderUri(CloudSyncSettings settings) {
    final endpoint = settings.endpoint.trim();
    final normalized = endpoint.endsWith('/') ? endpoint : '$endpoint/';
    final base = Uri.parse(normalized);
    final segments = base.pathSegments.where((segment) => segment.isNotEmpty);
    if (segments.isNotEmpty && segments.last == syncFolderName) {
      return base;
    }
    return base.resolve('$syncFolderName/');
  }
}
