package com.b8box.service;

import com.b8box.model.User;
import com.b8box.model.UserAvatar;
import com.b8box.repository.UserAvatarRepository;
import com.b8box.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

/**
 * Foto de perfil: valida o arquivo enviado e guarda uma versão "limpa".
 *
 * Nunca guardamos o arquivo original. A imagem é decodificada e redesenhada
 * do zero (recorte quadrado no centro, 256x256, JPEG), o que:
 *  - garante que é uma imagem de verdade (e não outro arquivo renomeado);
 *  - descarta metadados como EXIF/localização GPS da foto do celular;
 *  - deixa todas as fotos com o mesmo tamanho e peso pequeno (~10-30 KB).
 */
@Service
public class AvatarService {

    public static final long MAX_BYTES = 2 * 1024 * 1024;          // 2 MB
    private static final int MAX_SOURCE_DIMENSION = 6000;           // px
    private static final int OUTPUT_SIZE = 256;                     // px
    private static final float JPEG_QUALITY = 0.85f;
    private static final Set<String> ALLOWED_FORMATS = Set.of("jpeg", "png");

    @Autowired
    private UserAvatarRepository avatarRepository;

    @Autowired
    private UserRepository userRepository;

    // URL pública da foto, ou null se a pessoa não tem foto.
    // O "?v=" muda a cada troca, então o navegador pode guardar a imagem em
    // cache por muito tempo sem nunca mostrar uma foto antiga.
    public static String avatarUrl(User u) {
        if (u == null || u.getAvatarUpdatedAt() == null) {
            return null;
        }
        long version = u.getAvatarUpdatedAt().toEpochSecond(ZoneOffset.UTC);
        return "/api/public/users/" + u.getId() + "/avatar?v=" + version;
    }

    @Transactional(readOnly = true)
    public Optional<UserAvatar> find(Long userId) {
        return avatarRepository.findById(userId);
    }

    // Valida, normaliza e salva. Lança IllegalArgumentException com uma
    // mensagem amigável quando o arquivo não serve.
    @Transactional
    public String save(User user, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma imagem foi enviada.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("A imagem deve ter no máximo 2 MB.");
        }

        byte[] jpeg;
        try {
            BufferedImage source = readImage(file.getBytes());
            jpeg = toSquareJpeg(source);
        } catch (IOException e) {
            throw new IllegalArgumentException("Não foi possível ler a imagem. Envie um arquivo JPG ou PNG.");
        }

        LocalDateTime now = LocalDateTime.now();
        UserAvatar avatar = avatarRepository.findById(user.getId()).orElseGet(() -> new UserAvatar(user.getId()));
        avatar.setImage(jpeg);
        avatar.setContentType("image/jpeg");
        avatar.setUpdatedAt(now);
        avatarRepository.save(avatar);

        user.setAvatarUpdatedAt(now);
        userRepository.save(user);
        return avatarUrl(user);
    }

    @Transactional
    public void delete(User user) {
        if (avatarRepository.existsById(user.getId())) {
            avatarRepository.deleteById(user.getId());
        }
        if (user.getAvatarUpdatedAt() != null) {
            user.setAvatarUpdatedAt(null);
            userRepository.save(user);
        }
    }

    // ============================================================
    // AUXILIARES DE IMAGEM
    // ============================================================

    // Lê o formato e as dimensões ANTES de decodificar os pixels: uma imagem
    // pequena em bytes mas gigante em pixels (ex.: 30000x30000) poderia
    // ocupar gigabytes de memória ao ser aberta.
    private BufferedImage readImage(byte[] bytes) throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (in == null) {
                throw new IOException("stream inválido");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                throw new IOException("formato desconhecido");
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase();
                if (!ALLOWED_FORMATS.contains(format)) {
                    throw new IllegalArgumentException("Formato não aceito. Envie um arquivo JPG ou PNG.");
                }
                reader.setInput(in, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width > MAX_SOURCE_DIMENSION || height > MAX_SOURCE_DIMENSION) {
                    throw new IllegalArgumentException("A imagem é grande demais (máximo 6000x6000 pixels).");
                }
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw new IOException("imagem vazia");
                }
                return image;
            } finally {
                reader.dispose();
            }
        }
    }

    // Recorta o quadrado central e redimensiona para OUTPUT_SIZE.
    // O fundo branco evita que partes transparentes de um PNG virem preto no JPEG.
    private byte[] toSquareJpeg(BufferedImage source) throws IOException {
        int side = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - side) / 2;
        int y = (source.getHeight() - side) / 2;

        BufferedImage out = new BufferedImage(OUTPUT_SIZE, OUTPUT_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, OUTPUT_SIZE, OUTPUT_SIZE);
            g.drawImage(source, 0, 0, OUTPUT_SIZE, OUTPUT_SIZE, x, y, x + side, y + side, null);
        } finally {
            g.dispose();
        }

        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            writer.write(null, new IIOImage(out, null, null), param);
        } finally {
            writer.dispose();
        }
        return bytes.toByteArray();
    }
}
