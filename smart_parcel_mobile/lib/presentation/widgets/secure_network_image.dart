import 'package:flutter/material.dart';

import '../../core/storage/token_storage.dart';

/// Loads an image that may require the authenticated access token.
class SecureNetworkImage extends StatefulWidget {
  const SecureNetworkImage({
    super.key,
    required this.imageUrl,
    this.fit,
    this.errorText = '이미지를 불러올 수 없습니다.',
    this.placeholder,
  });

  final String imageUrl;
  final BoxFit? fit;
  final String errorText;
  final Widget? placeholder;

  @override
  State<SecureNetworkImage> createState() => _SecureNetworkImageState();
}

class _SecureNetworkImageState extends State<SecureNetworkImage> {
  late final Future<String?> _tokenFuture = TokenStorage().getAccessToken();

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<String?>(
      future: _tokenFuture,
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return widget.placeholder ??
              const Center(child: CircularProgressIndicator(strokeWidth: 2));
        }
        if (snapshot.hasError) {
          return Center(child: Text(widget.errorText));
        }

        final token = snapshot.data;
        return Image.network(
          widget.imageUrl,
          fit: widget.fit,
          key: ValueKey('${widget.imageUrl}-${token ?? 'public'}'),
          headers: token != null && token.isNotEmpty
              ? {'Authorization': 'Bearer $token'}
              : null,
          errorBuilder: (_, __, ___) => Center(child: Text(widget.errorText)),
        );
      },
    );
  }
}
