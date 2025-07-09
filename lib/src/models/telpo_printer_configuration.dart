class TelpoPrinterConfiguration {
  final dynamic fontFilePath;

  const TelpoPrinterConfiguration({
    required this.fontFilePath,
  });

  /// Data is being transferred to the plugin as JSON.
  Map<String, dynamic> toJson() {
    return {
      "fontFilePath": fontFilePath,
    };
  }

  /// Stringifying [PrintData] object.
  @override
  String toString() {
    return '''TelpoPrinterConfiguration(
      fontFilePath: $fontFilePath,
      )''';
  }
}
