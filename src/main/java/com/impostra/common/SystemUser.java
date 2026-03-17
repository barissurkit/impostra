package com.impostra.common;

// "extends Role" diyerek bu sınıfın bir Rol olduğunu belirtiyoruz (Kalıtım/Inheritance)
public class SystemUser extends Role {

    // Yapıcı Metot: Köylü oluşturulduğunda ismini "Köylü" ve isEvil (kötü mü) durumunu "false" yapıyoruz.
    public SystemUser() {
        super("Kullanıcı", false);
    }

    // Mecburi ezeceğimiz (Override) gece aksiyonu metodu.
    @Override
    public void performNightAction() {
        // Düz köylünün gece yapacak bir şeyi yoktur, sadece uyur.
        System.out.println("Kullanıcı uyuyor... Zzz...");
    }
}