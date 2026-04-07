package com.assistencia.controller;

import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;
import com.assistencia.model.Empresa;
import com.assistencia.model.Usuario;
import com.assistencia.repository.EmpresaRepository;
import com.assistencia.repository.UsuarioRepository;
import com.assistencia.service.EmailService;
import com.assistencia.util.CpfValidator;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @GetMapping("/login")
    public String login(Model model) {
        if (!model.containsAttribute("usuario")) {
            model.addAttribute("usuario", new Usuario());
        }
        model.addAttribute("empresas", empresaRepository.findByAtivoTrue());
        return "login";
    }

    @GetMapping("/registro-funcionario")
    public String telaRegistroFuncionario(Model model) {
        if (!model.containsAttribute("usuario")) {
            model.addAttribute("usuario", new Usuario());
        }
        model.addAttribute("empresas", empresaRepository.findByAtivoTrue());
        return "criarfuncionario";
    }

    @GetMapping("/registro-empresa")
    public String telaRegistroEmpresa(Model model) {
        if (!model.containsAttribute("usuario")) {
            model.addAttribute("usuario", new Usuario());
        }
        return "criarempresa";
    }

    @PostMapping("/registro")
    public String registrarFuncionario(@Valid @ModelAttribute("usuario") Usuario usuario,
                                       BindingResult result,
                                       @RequestParam(required = false) Long empresaId,
                                       Model model,
                                       RedirectAttributes attributes) {

        if (empresaId == null) {
            result.rejectValue("empresa", "error.usuario", "Informação obrigatória");
        }

        if (usuario.getWhatsapp() == null || usuario.getWhatsapp().isBlank()) {
            result.rejectValue("whatsapp", "error.usuario", "Informação obrigatória");
        } else {
            String wppLimpo = usuario.getWhatsapp().replaceAll("\\D", "");
            if (wppLimpo.length() != 11) {
                result.rejectValue("whatsapp", "error.usuario", "WhatsApp inválido (deve ter 11 dígitos)");
            }
        }

        if (usuario.getCpf() == null || usuario.getCpf().isBlank()) {
            result.rejectValue("cpf", "error.usuario", "Informação obrigatória");
        } else if (!CpfValidator.isValid(usuario.getCpf())) {
            result.rejectValue("cpf", "error.usuario", "CPF inválido");
        }

        validarDuplicidade(usuario, result);

        if (result.hasErrors()) {
            model.addAttribute("empresas", empresaRepository.findByAtivoTrue());
            model.addAttribute("empresaId", empresaId);
            return "criarfuncionario";
        }

        try {
            Empresa emp = empresaRepository.findById(empresaId)
                    .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));

            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            usuario.setRole("ROLE_FUNCIONARIO");
            usuario.setAprovado(false);
            usuario.setEmpresa(emp);

            usuarioRepository.save(usuario);
            attributes.addFlashAttribute("mensagemSucesso", "Conta de funcionário criada com sucesso! Agora você precisa ser aprovado pelo proprietário da empresa.");

        } catch (Exception e) {
            model.addAttribute("mensagemErro", "Erro ao processar solicitação.");
            model.addAttribute("empresas", empresaRepository.findByAtivoTrue());
            return "criarfuncionario";
        }
        return "redirect:/login";
    }

    @PostMapping("/esqueci-senha")
    public String processarRecuperacao(@RequestParam("identificador") String iden, RedirectAttributes attributes) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsernameOrEmailOrWhatsapp(iden, iden, iden);

        if (usuarioOpt.isPresent()) {
            Usuario user = usuarioOpt.get();
            try {
                String token = UUID.randomUUID().toString();
                user.setResetPasswordToken(token);
                user.setTokenExpiration(LocalDateTime.now().plusHours(1));
                usuarioRepository.save(user);

                emailService.enviarEmailRecuperacao(user.getEmail(), token);
                attributes.addFlashAttribute("sucessoRecuperacao", "E-mail enviado para " + user.getEmail());
            } catch (Exception e) {
                attributes.addFlashAttribute("mensagemErro", "Erro ao enviar e-mail.");
            }
        } else {
            attributes.addFlashAttribute("mensagemErro", "Usuário não encontrado!");
        }
        return "redirect:/login";
    }

    @GetMapping("/resetar-senha")
    public String exibirTelaNovaSenha(@RequestParam("token") String token, Model model, RedirectAttributes attributes) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByResetPasswordToken(token);

        if (usuarioOpt.isPresent() && usuarioOpt.get().getTokenExpiration().isAfter(LocalDateTime.now())) {
            model.addAttribute("token", token);
            return "novasenha";
        }

        attributes.addFlashAttribute("mensagemErro", "Link de recuperação inválido ou expirado.");
        return "redirect:/login";
    }

    @PostMapping("/atualizar-senha")
    public String atualizarSenha(@RequestParam("token") String token,
                                 @RequestParam("password") String novaSenha,
                                 RedirectAttributes attributes) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByResetPasswordToken(token);

        if (usuarioOpt.isPresent() && usuarioOpt.get().getTokenExpiration().isAfter(LocalDateTime.now())) {
            Usuario usuario = usuarioOpt.get();
            usuario.setPassword(passwordEncoder.encode(novaSenha));
            usuario.setResetPasswordToken(null);
            usuario.setTokenExpiration(null);
            usuarioRepository.save(usuario);
            attributes.addFlashAttribute("mensagemSucesso", "Senha atualizada! Faça login agora.");
            return "redirect:/login";
        }

        attributes.addFlashAttribute("mensagemErro", "Erro ao atualizar senha. Link expirado.");
        return "redirect:/login";
    }

    @PostMapping("/registro-empresa")
    public String registrarEmpresa(@RequestParam(required = false) String nomeEmpresa,
                                   @RequestParam(required = false) String cnpj,
                                   @Valid @ModelAttribute("usuario") Usuario usuario,
                                   BindingResult result,
                                   Model model,
                                   RedirectAttributes attributes) {

        if (nomeEmpresa == null || nomeEmpresa.isBlank()) {
            result.rejectValue("empresa.nome", "error.usuario", "Informação obrigatória");
        }

        // Validação básica de CNPJ duplicado (opcional, mas recomendado)
        if (cnpj != null && !cnpj.isBlank()) {
            String cnpjLimpo = cnpj.replaceAll("\\D", "");
            if (empresaRepository.findByCnpj(cnpjLimpo).isPresent()) {
                result.rejectValue("empresa.nome", "error.usuario", "Este CNPJ já está cadastrado");
            }
        }

        if (usuario.getWhatsapp() == null || usuario.getWhatsapp().isBlank()) {
            result.rejectValue("whatsapp", "error.usuario", "Informação obrigatória");
        } else {
            String wppLimpo = usuario.getWhatsapp().replaceAll("\\D", "");
            if (wppLimpo.length() != 11) {
                result.rejectValue("whatsapp", "error.usuario", "WhatsApp inválido (deve ter 11 dígitos)");
            }
        }

        if (usuario.getCpf() == null || usuario.getCpf().isBlank()) {
            result.rejectValue("cpf", "error.usuario", "Informação obrigatória");
        } else if (!CpfValidator.isValid(usuario.getCpf())) {
            result.rejectValue("cpf", "error.usuario", "CPF inválido");
        }

        validarDuplicidade(usuario, result);

        if (result.hasErrors()) {
            model.addAttribute("nomeEmpresaDigitado", nomeEmpresa);
            model.addAttribute("cnpjDigitado", cnpj);
            return "criarempresa";
        }

        try {
            Empresa novaEmpresa = new Empresa();
            novaEmpresa.setNome(nomeEmpresa);

            if (cnpj != null && !cnpj.isBlank()) {
                novaEmpresa.setCnpj(cnpj.replaceAll("\\D", ""));
            }

            novaEmpresa.setAtivo(true);
            novaEmpresa.setDiasRestantes(7);
            Empresa empresaSalva = empresaRepository.save(novaEmpresa);

            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            usuario.setRole("ROLE_ADMIN");
            usuario.setTipoFuncionario("PROPRIETARIO");
            usuario.setAprovado(true);
            usuario.setEmpresa(empresaSalva);

            usuarioRepository.save(usuario);
            attributes.addFlashAttribute("mensagemSucesso", "Empresa criada com sucesso! Você ganhou (7) dias de acesso grátis!");

        } catch (Exception e) {
            model.addAttribute("mensagemErro", "Erro interno ao criar empresa.");
            model.addAttribute("nomeEmpresaDigitado", nomeEmpresa);
            return "criarempresa";
        }
        return "redirect:/login";
    }

    private void validarDuplicidade(Usuario usuario, BindingResult result) {
        if (usuarioRepository.findByUsername(usuario.getUsername()).isPresent()) {
            result.rejectValue("username", "error.usuario", "Login já em uso");
        }
        if (usuario.getEmail() != null && !usuario.getEmail().isBlank()) {
            usuarioRepository.findByEmail(usuario.getEmail()).ifPresent(u -> {
                result.rejectValue("email", "error.usuario", "E-mail já em uso");
            });
        }
        if (usuario.getWhatsapp() != null && !usuario.getWhatsapp().isBlank()) {
            usuarioRepository.findByWhatsapp(usuario.getWhatsapp()).ifPresent(u -> {
                result.rejectValue("whatsapp", "error.usuario", "Este WhatsApp já está sendo usado");
            });
        }
    }
}